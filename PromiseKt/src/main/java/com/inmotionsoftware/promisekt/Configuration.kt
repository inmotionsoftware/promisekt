package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.Executors

object PMKConfiguration {
    data class Value(val map: Executor?, val `return`: Executor?)

    var Q: Value = Value(map = Executors.newCachedThreadPool(), `return` = Executors.newCachedThreadPool())
    var catchPolicy: CatchPolicy = CatchPolicy.allErrorsExceptCancellation
}

var conf = PMKConfiguration
