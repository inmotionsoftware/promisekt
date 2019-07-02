package com.inmotionsoftware.promisekt

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

internal object DispatchExecutor {
    val main: Executor by lazy { Executors.newSingleThreadExecutor() }
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}

/**
 * Asynchronously executes the provided closure on an Executor.
 *
 *  DispatchExecutor.global().async {
 *      // a long runing task
 *  }
 *
 * @param body: The closure to run on the Executor
 */
internal fun Executor?.async(body: () -> Unit) {
    when (this) {
        null -> body()
        else -> execute(body)
    }
}

/**
 * Asynchronously executes the provided closure on an Executor.
 *
 *  DispatchExecutor.global().async(.promise) {
 *      1
 *  }.done {
 *           //…
 *  }
 *
 * @param body: The closure that resolves this promise.
 * @return A new `Promise` resolved by the result of the provided closure.
 */
internal fun <T> Executor?.async(namespace: PMKNamespacer, body: () -> T): Promise<T> {
    val promise = Promise<T>(PMKUnambiguousInitializer.pending)
    async {
        try {
            promise.box.seal(Result.fulfilled(body()))
        } catch( e: Throwable) {
            promise.box.seal(Result.rejected(e))
        }
    }
    return promise
}

internal fun Executor.asyncAfter(seconds: Double, invoke: () -> Unit) {
    execute {
        Thread.sleep((max(seconds, 0.0) * 1000).toLong())
        invoke()
    }
}

internal fun Executor?.sync(body: () -> Unit) {
    when (this) {
        null -> body()
        else -> {
            val countDown = CountDownLatch(1)
            execute {
                synchronized(lock = this) { body() }
                countDown.countDown()
            }
            countDown.await()
        }
    }
}
