package com.inmotionsoftware.promisekt

import java.util.concurrent.CountDownLatch

class Promise<T>: Thenable<T>, CatchMixin<T> {
    internal val box: Box<Result<T>>

    internal constructor(box: SealedBox<Result<T>>) {
        this.box = box
    }

    constructor(unambiguous: PMKUnambiguousInitializer) {
        this.box = EmptyBox()
    }

    // Initialize a new rejected promise.
    constructor(error: Throwable) {
        this.box = SealedBox(value = Result.rejected(error))
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
    var result = this.result
    if (result == null) {
        val latch = CountDownLatch(1)
        pipe {
            result = it
            latch.countDown()
        }
        latch.await()
    }

    val r = result!!
    return when (r) {
        is Result.fulfilled -> r.value
        is Result.rejected -> throw r.error
    }
}

/// used by our extensions to provide unambiguous functions with the same name as the original function
enum class PMKNamespacer {
    promise
}

enum class PMKUnambiguousInitializer {
    pending
}
