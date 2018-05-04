package com.inmotionsoftware.promisekt

import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AsyncTests {

    fun wait(countDown: CountDownLatch, timeout: Long, timeUnit: TimeUnit = TimeUnit.SECONDS) {
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            countDown.await()
        }
        try {
            executor.shutdown()
            executor.awaitTermination(timeout, timeUnit)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

}
