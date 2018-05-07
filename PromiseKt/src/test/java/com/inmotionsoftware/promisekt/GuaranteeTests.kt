package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.features.firstly
import com.inmotionsoftware.promisekt.features.firstlyGuarantee
import org.junit.Assert
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
    fun testThen() {
        val e = CountDownLatch(1)
        Guarantee.value(1).thenGuarantee {
            assertEquals(1, it)
            Guarantee.value(2)
        }.done {
            assertEquals(2, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testDone() {
        val e = CountDownLatch(1)
        Guarantee.value(1).thenGuarantee {
            assertEquals(1, it)
            Guarantee.value(2)
        }.guaranteeDone {
            assertEquals(2, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testAsVoid() {
        val e = CountDownLatch(1)
        Guarantee.value(1).asGuaranteeVoid().done { e.countDown() }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testFirstly() {
        val e = CountDownLatch(1)

        firstlyGuarantee {
            Guarantee.value(1)
        }.done {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testWait() {
        assertEquals(after(.1).map(on = null){ 1 }.wait(), 1)
    }

}
