package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

object PMKConfiguration {
    data class Value(val map: Executor?, val `return`: Executor?)

    var Q: Value = Value(map = DispatchExecutor.main, `return` = DispatchExecutor.main)
    var catchPolicy: CatchPolicy = CatchPolicy.allErrorsExceptCancellation
}

var conf = PMKConfiguration
