package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.after
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AfterTests: AsyncTests() {

    @Test
    fun testZero() {
        val e = CountDownLatch(1)
        after(seconds = 0.0).done { e.countDown() }
        wait(countDown = e, timeout = 2)
    }

    @Test
    fun testNegative() {
        val e = CountDownLatch(1)
        after(seconds = -1.0).done { e.countDown() }
        wait(countDown = e, timeout = 2)
    }

    @Test
    fun testPositive()  {
        val e = CountDownLatch(1)
        after(seconds = 1.0).done { e.countDown() }
        wait(countDown = e, timeout = 2)
    }

}
