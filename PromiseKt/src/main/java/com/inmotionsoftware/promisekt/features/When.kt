package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private fun <T, U: Thenable<T>> _when(thenables: Iterable<U>, executor: ExecutorService = Executors.newCachedThreadPool()): Promise<Unit> {
    val countdown = AtomicInteger(thenables.count())
    if (countdown.get() == 0) return Promise.value(Unit)

    val rp = Promise<Unit>(PMKUnambiguousInitializer.pending)
    val lock = Object()

    thenables.forEach { promise ->
        promise.pipe { result ->
            executor.submit {
                synchronized(lock = lock) {
                    when (result) {
                        is Result.rejected -> {
                            if (rp.isPending) {
                                rp.box.seal(Result.rejected(result.error))
                            }
                        }
                        is Result.fulfilled -> {
                            if (rp.isPending && countdown.decrementAndGet() == 0) {
                                rp.box.seal(Result.fulfilled(Unit))
                            }
                        }
                    }
                }
            }
        }
    }
    try {
        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    } catch (e: Throwable) {
        rp.box.seal(Result.rejected(e))
    }
    return rp
}

/**
 * Wait for all promises in a set to fulfill.
 *
 * For example:
 *
 *   when(fulfilled: promise1, promise2).then { results in
 *      //...
 *   }.catch { error ->
 *      when(error) {
 *          is SomeError.type -> { ... }
 *          else -> { ... }
 *      }
 *   }
 *
 * Note: If *any* of the provided promises reject, the returned promise is immediately rejected with that error.
 * Warning: In the event of rejection the other promises will continue to resolve and, as per any other promise, will either fulfill or reject. This is the right pattern for `getter` style asynchronous tasks, but often for `setter` tasks (eg. storing data on a server), you most likely will need to wait on all tasks and then act based on which have succeeded and which have failed, in such situations use `when(resolved:)`.
 *
 * @param thenables: The promises upon which to wait before the returned promise resolves.
 * @return: A new promise that resolves when all the provided promises fulfill or one of the provided promises rejects.
 */
fun <T, U: Thenable<T>> whenFulfilled(thenables: Iterable<U>): Promise<Iterable<T>> {
    return _when(thenables).map(on = null) { thenables.map { it.value!! } }
}

/**
 * Generate promises at a limited rate and wait for all to fulfill.
 *    when(promises, concurrently: 3).done { datas ->
 *       // ...
 *    }
 *
 * No more than three downloads will occur simultaneously.
 *
 * @param thenables: The promises upon which to wait before the returned promise resolves.
 * @param concurrently: The number of concurent promises to execute simultaneously
 * @return: A new promise that resolves when all the provided promises fulfill or one of the provided promises rejects.
 */
fun <T, U: Thenable<T>> whenFulfilled(promises: Iterable<U>, concurrently: Int): Promise<Iterable<T>> {
    if (concurrently <= 0) return Promise(error = PMKError.badInput())

    val generator = promises.iterator()
    val root = Promise.pending<Iterable<T>>()
    val pendingPromises = AtomicInteger(0)
    val promiseList: ArrayList<U> = arrayListOf()

    fun dequeue() {
        if (!root.first.isPending) return // don’t continue dequeueing if root has been rejected

        var shouldDequeue = false
        DispatchExecutor.global.sync {
            shouldDequeue = pendingPromises.get() < concurrently
        }
        if (!shouldDequeue) return

        var promise: U? = null

        DispatchExecutor.global.sync {
            if (generator.hasNext()) {
                val next = generator.next()
                promise = next

                pendingPromises.incrementAndGet()
                promiseList.add(next)
            }
        }

        fun testDone() {
            DispatchExecutor.global.sync {
                if (pendingPromises.get() == 0) {
                    root.second.fulfill(promiseList.flatMap{
                        if (it.value != null) arrayListOf(it.value!!) else arrayListOf()
                    })
                }
            }
        }

        if (promise == null) {
            return testDone()
        }

        promise?.pipe { resolution ->
            DispatchExecutor.global.sync {
                pendingPromises.decrementAndGet()
            }

            when (resolution) {
                is Result.fulfilled -> {
                    dequeue()
                    testDone()
                }
                is Result.rejected -> {
                    root.second.reject(resolution.error)
                }
            }
        }
        dequeue()
    }
    dequeue()
    return root.first
}

/**
 * Wait for all promises in a set to fulfill.
 */
fun <T, U: Thenable<T>> whenFulfilled(vararg promises: U): Promise<Iterable<T>> {
    return whenFulfilled(promises.asIterable())
}

/**
 * Wait for all promises in a set to fulfill.
 */
fun <TU, U: Thenable<TU>, TV, V: Thenable<TV>> whenFulfilled(pu: U, pv: V): Promise<Pair<TU, TV>> {
    return _when(arrayListOf(pu.asVoid(), pv.asVoid())).map(on = null) { Pair(pu.value!!, pv.value!!) }
}

/**
 * Wait for all promises in a set to fulfill.
 */
fun <TU, U: Thenable<TU>, TV, V: Thenable<TV>, TW, W: Thenable<TW>> whenFulfilled(pu: U, pv: V, pw: W): Promise<Triple<TU, TV, TW>> {
    return _when(arrayListOf(pu.asVoid(), pv.asVoid(), pw.asVoid())).map(on = null) { Triple(pu.value!!, pv.value!!, pw.value!!) }
}

/**
 * Waits on all provided promises.
 *
 * `when(fulfilled:)` rejects as soon as one of the provided promises rejects. `when(resolved:)` waits on all provided promises and **never** rejects.
 *
 * @return: A new promise that resolves once all the provided promises resolve. The list is ordered the same as the input, ie. the result order is *not* resolution order.
 * Warning: The returned promise can *not* be rejected.
 * Note: Any promises that error are implicitly consumed.
 */
fun <T> whenResolved(vararg promises: Promise<T>): Guarantee<Iterable<Result<T>>> {
    return whenResolved(promises.asIterable())
}

/**
 * Waits on all provided promises.
 */
fun <T> whenResolved(promises: Iterable<Promise<T>>): Guarantee<Iterable<Result<T>>> {
    if (promises.count() == 0) return Guarantee.value(arrayListOf())
    val countdown = AtomicInteger(promises.count())

    val rg = Guarantee.pending<Iterable<Result<T>>>()
    promises.forEach { promise ->
        promise.pipe {
            if (countdown.decrementAndGet() == 0) {
                rg.first.box.seal(promises.map { it.result!! })
            }
        }
    }
    return rg.first
}

/**
 * Waits on all provided Guarantees.
 */
fun whenGuarantee(vararg guarantees: Guarantee<Unit>): Guarantee<Unit> {
    return whenGuarantee(guarantees.asIterable())
}

/**
 * Waits on all provided Guarantees.
 */
fun whenGuarantee(guarantees: Iterable<Guarantee<Unit>>): Guarantee<Unit> {
    return whenFulfilled(guarantees).recoverGuarantee { Guarantee.value(arrayListOf()) }.mapGuarantee { }
}
