package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

class CurrentThreadExecutor: Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}

object DispatchExecutor {
    val main: Executor = CurrentThreadExecutor()
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}

fun Executor.asyncAfter(seconds: Double, invoke: () -> Unit) {
    execute {
        Thread.sleep((max(seconds, 0.0) * 1000).toLong())
        invoke()
    }
}
