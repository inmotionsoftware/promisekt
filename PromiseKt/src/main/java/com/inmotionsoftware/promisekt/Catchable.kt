package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

interface CatchMixin<T>: Thenable<T>

fun <T> CatchMixin<T>.catch(on: Executor? = PMKConfiguration.Q.`return`, policy: CatchPolicy = PMKConfiguration.catchPolicy, body: (Throwable) -> Unit): PMKFinalizer {
    val finalizer = PMKFinalizer()
    pipe {
        when (it) {
            is Result.rejected -> {
                if (policy == CatchPolicy.allErrors || !it.error.isCancelled) {
                    on.async {
                        body(it.error)
                        finalizer.pending.second(Unit)
                    }
                } else {
                    finalizer.pending.second(Unit)
                }
            }
            is Result.fulfilled -> {
                finalizer.pending.second(Unit)
            }
        }
    }
    return finalizer
}

class PMKFinalizer {
    val pending = Guarantee.pending<Unit>()

    // `finally` is the same as `ensure`, but it is not chainable
    fun finally(body: (Unit) -> Unit) {
        pending.first.done(body = body)
    }
}

fun <T, U: Thenable<T>> CatchMixin<T>.recover(on: Executor? = PMKConfiguration.Q.map, policy: CatchPolicy = PMKConfiguration.catchPolicy, body: (Throwable) -> U): Promise<T> {
    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                rp.box.seal(Result.fulfilled(it.value))
            }
            is Result.rejected -> {
                if (policy == CatchPolicy.allErrors || !it.error.isCancelled) {
                    on.async {
                        try {
                            val rv = body(it.error)
                            if (rv === rp) { throw PMKError.returnedSelf() }
                            rv.pipe(to = rp.box::seal)
                        } catch (e: Throwable) {
                            rp.box.seal(Result.rejected(it.error))
                        }
                    }
                } else {
                    rp.box.seal(Result.rejected(it.error))
                }
            }
        }
    }
    return rp
}

fun <T> CatchMixin<T>.recoverGuarantee(on: Executor? = PMKConfiguration.Q.map, body: (Throwable) -> Guarantee<T>): Guarantee<T> {
    val rg = Guarantee<T>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                rg.box.seal(it.value)
            }
            is Result.rejected -> {
                on.async {
                    body(it.error).pipeTo(rg.box::seal)
                }
            }
        }
    }
    return rg
}

fun <T> CatchMixin<T>.ensure(on: Executor? = PMKConfiguration.Q.`return`, body: () -> Unit): Promise<T> {
    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    pipe { result ->
        on.async {
            body()
            rp.box.seal(result)
        }
    }
    return rp
}

fun <T> CatchMixin<T>.ensureThen(on: Executor? = PMKConfiguration.Q.`return`, body: () -> Guarantee<Unit>): Promise<T> {
    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    pipe { result ->
        on.async {
            body().done {
                rp.box.seal(result)
            }
        }
    }
    return rp
}

fun <T> CatchMixin<T>.cauterize() {
    catch {
        println("PromiseKot:cauterized-error: $it")
    }
}
