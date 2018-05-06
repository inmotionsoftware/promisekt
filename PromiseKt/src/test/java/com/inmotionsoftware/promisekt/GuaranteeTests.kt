package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch

class GuaranteeTests: AsyncTests() {

    @Test
    fun testInit() {
        val e = CountDownLatch(1)
        Guarantee<Int> { seal ->
            seal(1)
        }.done {
            assertEquals(1, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testWait() {
        assertEquals(after(.1).map(on = null){ 1 }.wait(), 1)
    }

}
