package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

/**
 * Provides `catch` and `recover` to your object that conforms to `Thenable`
 */
interface CatchMixin<T>: Thenable<T>

/**
 * The provided closure executes when this promise rejects.
 *
 * Rejecting a promise cascades: rejecting all subsequent promises (unless
 * recover is invoked) thus you will typically place your catch at the end
 * of a chain. Often utility promises will not have a catch, instead
 * delegating the error handling to the caller.
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param policy: The default policy does not execute your handler for cancellation errors.
 * @param body: The handler to execute if this promise is rejected.
 * @return A promise finalizer.
 */
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

    /**
     * `finally` is the same as `ensure`, but it is not chainable
     */
    fun finally(body: (Unit) -> Unit) {
        pending.first.done(body = body)
    }
}

/**
 * The provided closure executes when this promise rejects.
 *
 * Unlike `catch`, `recover` continues the chain.
 * Use `recover` in circumstances where recovering the chain from certain errors is a possibility. For example:
 *
 *   firstly {
 *      throw error
 *   }.recover {
 *     return Promise.value(1)
 *   }
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The handler to execute if this promise is rejected.
 */
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

/**
 * The provided closure executes when this promise rejects.
 *
 * This variant of `recover` requires the handler to return a Guarantee, thus it returns a Guarantee itself and your closure cannot `throw`.
 * Note it is logically impossible for this to take a `catchPolicy`, thus `allErrors` are handled.
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The handler to execute if this promise is rejected.
 */
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

/**
 * The provided closure executes when this promise resolves, whether it rejects or not.
 *
 *    firstly {
 *       showNetworkActivityIndicator(true)
 *    }.done {
 *       //...
 *    }.ensure {
 *       showNetworkActivityIndicator(false)
 *    }.catch {
 *       //...
 *    }
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The closure that executes when this promise resolves.
 * @return A new promise, resolved with this promise’s resolution.
 */
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

/**
 * The provided closure executes when this promise resolves, whether it rejects or not.
 * The chain waits on the returned `Guarantee<Unit>`.
 *
 *    firstly {
 *       setup()
 *    }.done {
 *       //...
 *    }.ensureThen {
 *       teardown()  // -> Guarante<Unit>
 *    }.catch {
 *      //...
 *    }
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The closure that executes when this promise resolves.
 * @return A new promise, resolved with this promise’s resolution.
 */
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

/**
 * Provide this to be compatible with the Swift's version, but is not needed in Kotlin.
 * Note: You should `catch`, but in situations where you know you don’t need a `catch`, `cauterize` makes your intentions clear.
 */
fun <T> CatchMixin<T>.cauterize() {
    catch {
        println("PromiseKot:cauterized-error: $it")
    }
}
