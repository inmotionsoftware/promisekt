package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CurrentThreadExecutor: Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}

object PMKConfiguration {
    data class Value(val map: Executor?, val `return`: Executor?)
    private val executor = CurrentThreadExecutor()

    var Q: Value = Value(map = executor, `return` = executor)
    var catchPolicy: CatchPolicy = CatchPolicy.allErrorsExceptCancellation
}

var conf = PMKConfiguration
