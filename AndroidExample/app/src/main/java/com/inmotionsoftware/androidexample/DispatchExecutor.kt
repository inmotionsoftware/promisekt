package com.inmotionsoftware.androidexample

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object DispatchExecutor {
    val main: Executor by lazy { Executor { command -> Handler(Looper.getMainLooper()).post(command) } }
    val background: Executor by lazy { Executors.newCachedThreadPool() }
}