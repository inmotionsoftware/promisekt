package com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

private fun <T, U: Thenable<T>> _when(thenables: Iterable<U>, executor: ExecutorService = Executors.newCachedThreadPool()): Promise<Unit> {
    val countdown = AtomicInteger(thenables.count())
    if (countdown.get() == 0) return Promise.value(Unit)

    val rp = Promise<Unit>(PMKUnambiguousInitializer.pending)

    thenables.forEach { promise ->
        promise.pipe { result ->
            executor.submit {
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
    try {
        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    } catch (e: Exception) {
        rp.box.seal(Result.rejected(e))
    }
    return rp
}

fun <T, U: Thenable<T>> whenFulfilled(thenables: Iterable<U>): Promise<Iterable<T>> {
    return _when(thenables).map(on = null) { thenables.map { it.value!! } }
}

fun <T, U: Thenable<T>> whenFulfilled(promises: Iterable<U>, concurrently: Int): Promise<Iterable<T>> {
    return _when(promises, Executors.newFixedThreadPool(min(promises.count(), concurrently))).map(on = null) { promises.map { it.value!! } }
}

fun <U: Thenable<Unit>> whenFulfilled(vararg promises: U): Promise<Unit> {
    return _when(promises.asIterable())
}

fun <TU, U: Thenable<TU>, TV, V: Thenable<TV>> whenFulfilled(pu: U, pv: V): Promise<Pair<TU, TV>> {
    return _when(arrayListOf(pu.asVoid(), pv.asVoid())).map(on = null) { Pair(pu.value!!, pv.value!!) }
}

fun <TU, U: Thenable<TU>, TV, V: Thenable<TV>, TW, W: Thenable<TW>> whenFulfilled(pu: U, pv: V, pw: W): Promise<Triple<TU, TV, TW>> {
    return _when(arrayListOf(pu.asVoid(), pv.asVoid(), pw.asVoid())).map(on = null) { Triple(pu.value!!, pv.value!!, pw.value!!) }
}

fun <T> whenResolved(vararg promises: Promise<T>): Guarantee<Iterable<Result<T>>> {
    return whenResolved(promises.asIterable())
}

fun <T> whenResolved(promises: Iterable<Promise<T>>): Guarantee<Iterable<Result<T>>> {
    if (promises.count() == 0) return Guarantee.value(arrayListOf())
    val countdown = AtomicInteger(promises.count())

    val rg = Guarantee.pending<Iterable<Result<T>>>()
    promises.forEach { promise ->
        promise.pipe { result ->
            if (countdown.decrementAndGet() == 0) {
                rg.first.box.seal(promises.map { it.result!! })
            }
        }
    }
    return rg.first
}

fun whenGuarantee(vararg guarantees: Guarantee<Unit>): Guarantee<Unit> {
    return whenGuarantee(guarantees.asIterable())
}

fun whenGuarantee(guarantees: Iterable<Guarantee<Unit>>): Guarantee<Unit> {
    return whenFulfilled(guarantees).guaranteeRecover { Guarantee.value(arrayListOf()) }.map {  }
}
