package com.inmotionsoftware.promisekt

class Promise<T>: Thenable<T>, CatchMixin<T> {
    internal val box: Box<Result<T>>

    internal constructor(box: SealedBox<Result<T>>) {
        this.box = box
    }

    constructor(unambiguous: PMKUnambiguousInitializer) {
        this.box = EmptyBox()
    }

    // Initialize a new promise bound to the provided `Thenable`.
    constructor(bridge: Thenable<T>) {
        this.box = EmptyBox()
        bridge.pipe(to = this.box::seal)
    }

    constructor(body: (Resolver<T>) -> Unit) {
        this.box = EmptyBox()
        val resolver = Resolver(this.box)
        try {
            body(resolver)
        } catch (e: Throwable) {
            resolver.reject(e)
        }
    }

    companion object {
        // Returns a new fulfilled promise.
        fun <T> value(value: T): Promise<T> {
            return Promise<T>(box = SealedBox(value = Result.fulfilled(value)))
        }

        // Returns a tuple of a new pending promise and its `Resolver`.
        fun <T> pending(): Pair<Promise<T>, Resolver<T>> {
            return { p: Promise<T> -> Pair(p, Resolver<T>(p.box)) }(Promise(PMKUnambiguousInitializer.pending))
        }

        // Returns a new rejected promise.
        fun <Throwable> error(error: Throwable): Promise<Throwable> {
            return Promise<Throwable>(box = SealedBox(value = Result.fulfilled(error)))
        }
    }

    /// Internal function required for `Thenable` conformance.
    override fun pipe(to: (Result<T>) -> Unit) {
        val sealant = this.box.inspect()
        when (sealant) {
            is Sealant.pending -> {
                this.box.inspect {
                    when (it) {
                        is Sealant.pending -> it.handlers.append(to)
                        is Sealant.resolved -> to(it.value)
                    }
                }
            }
            is Sealant.resolved -> to(sealant.value)
        }
    }

    // Returns the current `Result` for this promise.
    override val result: Result<T>? get() {
        val sealant = this.box.inspect()
        return when (sealant) {
            is Sealant.pending -> null
            is Sealant.resolved -> sealant.value
        }
    }
}

fun <T> Promise<T>.tap(body: (Result<T>) -> Unit): Promise<T> {
    pipe(to = body)
    return this
}

fun <T> Promise<T>.wait(): T {
    val lock = Object()

    var result = this.result
    synchronized(lock) {
        pipe {
            synchronized(lock) {
                result = it
                lock.notifyAll()
            }
        }
        lock.wait()
    }

    val r = result!!
    return when (r) {
        is Result.fulfilled -> r.value
        is Result.rejected -> throw r.error
    }
}

enum class PMKUnambiguousInitializer {
    pending
}
