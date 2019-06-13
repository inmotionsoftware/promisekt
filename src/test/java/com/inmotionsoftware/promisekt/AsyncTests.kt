package com.inmotionsoftware.promisekt

import org.junit.Assert.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AsyncTests {

    fun wait(countDown: CountDownLatch, timeout: Long, timeUnit: TimeUnit = TimeUnit.SECONDS) {
        val executor = Executors.newSingleThreadExecutor()
        executor.submit { countDown.await() }
        try {
            executor.shutdown()
            if (!executor.awaitTermination(timeout, timeUnit)) { fail("wait() timeout") }
        } catch (e: Throwable) {
            fail("wait() timeout")
        }
    }

}
