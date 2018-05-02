package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CurrentThreadExecutor: Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}

object DispatchExecutor {
    val main: Executor by lazy { CurrentThreadExecutor() }
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}

object PMKConfiguration {
    data class Value(val map: Executor?, val `return`: Executor?)

    var Q: Value = Value(map = DispatchExecutor.main, `return` = DispatchExecutor.main)
    var catchPolicy: CatchPolicy = CatchPolicy.allErrorsExceptCancellation
}

var conf = PMKConfiguration
