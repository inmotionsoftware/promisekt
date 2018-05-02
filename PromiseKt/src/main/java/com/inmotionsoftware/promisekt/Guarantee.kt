package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

// A `Guarantee` is a functional abstraction around an asynchronous operation that cannot error.
class Guarantee<T>: Thenable<T> {
    internal val box: Box<T>

    internal constructor(box: Box<T>) {
        this.box = box
    }

    constructor(unambiguous: PMKUnambiguousInitializer) {
        this.box = EmptyBox()
    }

    constructor(resolver: ((T) -> Unit) -> Unit) {
        this.box = EmptyBox<T>()
        resolver(box::seal)
    }

    companion object {
        fun <T> value(value: T): Guarantee<T> {
            return Guarantee<T>(SealedBox<T>(value))
        }

        fun <T> pending(): Pair<Guarantee<T>, (T) -> Unit> {
            return { g: Guarantee<T> -> Pair(g, g.box::seal) }(Guarantee(PMKUnambiguousInitializer.pending))
        }
    }

    override fun pipe(to: (Result<T>) -> Unit) {
        this.pipeTo { to(Result.fulfilled(it)) }
    }

    internal fun pipeTo(to: (T) -> Unit) {
        val sealant = this.box.inspect()
        when(sealant) {
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

    override val result: Result<T>? get() {
        val sealant = this.box.inspect()
        return when (sealant) {
            is Sealant.pending -> null
            is Sealant.resolved -> Result.fulfilled(sealant.value)
        }
    }
}

fun <T> Guarantee<T>.done(on: Executor? = conf.Q.`return`, body: (T) -> Unit): Guarantee<Unit> {
    val rg = Guarantee<Unit>(PMKUnambiguousInitializer.pending)
    pipeTo { value ->
        on.async {
            body(value)
            rg.box.seal(Unit)
        }
    }
    return rg
}

fun <T, U> Guarantee<T>.map(on: Executor? = conf.Q.map, body: (T) -> U): Guarantee<U> {
    val rg = Guarantee<U>(PMKUnambiguousInitializer.pending)
    pipeTo { value ->
        on.async {
            rg.box.seal(body(value))
        }
    }
    return rg
}

fun <T, U> Guarantee<T>.then(on: Executor? = conf.Q.map, body: (T) -> Guarantee<U>): Guarantee<U> {
    val rg = Guarantee<U>(PMKUnambiguousInitializer.pending)
    pipeTo { value ->
        on.async {
            body(value).pipeTo(rg.box::seal)
        }
    }
    return rg
}

fun Guarantee<Unit>.asVoid(): Guarantee<Unit> {
    return map(on = null) { }
}

fun <T> Guarantee<T>.wait(): T {
    val lock = Object()

    var result = this.value
    synchronized(lock) {
        pipeTo {
            synchronized(lock) {
                result = it
                lock.notifyAll()
            }
        }
        lock.wait()
    }
    return result!!
}
