package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CurrentThreadExecutor: Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}

object DispatchExecutor {
    val main: Executor = CurrentThreadExecutor()
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}
