package com.inmotionsoftware.promisekt

import java.util.concurrent.CountDownLatch
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

fun <T, U> Guarantee<T>.thenMap(on: Executor? = conf.Q.map, transform: (T) -> U): Promise<U> {
    val rp = Promise<U>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        rp.box.seal(Result.fulfilled(transform(it.value)))
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
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

fun <T, U> Guarantee<T>.thenPromise(on: Executor? = conf.Q.map, body: (T) -> Promise<U>): Promise<U> {
    val rp = Promise<U>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        val rv = body(it.value)
                        if (rv === rp) {
                            throw PMKError.returnedSelf()
                        }
                        rv.pipe(to = rp.box::seal)
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun Guarantee<Unit>.asVoid(): Guarantee<Unit> {
    return map(on = null) { }
}

fun <T> Guarantee<T>.wait(): T {
    var result = value
    if (result == null) {
        val latch = CountDownLatch(1)
        pipeTo {
            result = it
            latch.countDown()
        }
        latch.await()
    }
    return result!!
}
