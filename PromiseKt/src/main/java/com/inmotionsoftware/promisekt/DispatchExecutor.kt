package com.inmotionsoftware.promisekt

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

object DispatchExecutor {
    val main: Executor by lazy { Executors.newSingleThreadExecutor() }
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}

fun Executor?.async(body: () -> Unit) {
    when (this) {
        null -> body()
        else -> execute(body)
    }
}

fun Executor.asyncAfter(seconds: Double, invoke: () -> Unit) {
    execute {
        Thread.sleep((max(seconds, 0.0) * 1000).toLong())
        invoke()
    }
}

fun Executor?.sync(body: () -> Unit) {
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
