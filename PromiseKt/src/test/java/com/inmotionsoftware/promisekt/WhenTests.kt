package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenFulfilled
import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenGuarantee
import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenResolved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch

class WhenTests: AsyncTests() {

    private sealed class TestError: Throwable() {
        class test1: TestError()
        class test2: TestError()
        class test3: TestError()
    }

    @Test
    fun testEmpty() {
        val e = CountDownLatch(2)
        val promises: Iterable<Promise<Unit>> = emptyList()
        whenFulfilled(promises).done { e.countDown() }
        whenResolved(promises).done { e.countDown() }
        wait(countDown = e, timeout = 30)
    }

    @Test
    fun testInt() {
        val e = CountDownLatch(1)
        val p1: Promise<Int> = Promise.value(1)
        val p2: Promise<Int> = Promise.value(2)
        val p3: Promise<Int> = Promise.value(3)
        val p4: Promise<Int> = Promise.value(4)

        whenFulfilled(arrayListOf(p1, p2, p3, p4)).done {
            val values = it.toList()
            assertEquals(1, values[0])
            assertEquals(2, values[1])
            assertEquals(3, values[2])
            assertEquals(4, values[3])
            e.countDown()
        }
        wait(countDown = e, timeout = 30)
    }

    @Test
    fun testDoubleTuple() {
        val e = CountDownLatch(1)
        val p1 = Promise.value(1)
        val p2 = Promise.value("abc")
        whenFulfilled(p1, p2).done { pair ->
            assertEquals(pair.first, 1)
            assertEquals(pair.second, "abc")
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testTripleTuple() {
        val e = CountDownLatch(1)
        val p1 = Promise.value(1)
        val p2 = Promise.value("abc")
        val p3 = Promise.value(1.0)
        whenFulfilled(p1, p2, p3).done { triple ->
            assertEquals(triple.first, 1)
            assertEquals(triple.second, "abc")
            assertEquals(triple.third, 1.0)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testVoid() {
        val e = CountDownLatch(1)
        val p1 = Promise.value(1).done { }
        val p2 = Promise.value(2).done { }
        val p3 = Promise.value(3).done { }
        val p4 = Promise.value(4).done { }

        whenFulfilled(arrayListOf(p1, p2, p3, p4)).done { e.countDown() }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testAllSealedRejectedFirstOneRejects() {
        val e = CountDownLatch(1)
        val test1 = TestError.test1()
        val p1 = Promise<Void>(error = test1)
        val p2 = Promise<Void>(error = TestError.test2())
        val p3 = Promise<Void>(error = TestError.test3())

        whenFulfilled(p1, p2, p3).catch { error ->
            assertTrue(error == test1)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testGuaranteeWhen() {
        val e = CountDownLatch(2)
        whenGuarantee(Guarantee.value(Unit), Guarantee.value(Unit)).done { e.countDown() }
        whenGuarantee(arrayListOf(Guarantee.value(Unit), Guarantee.value(Unit))).done { e.countDown() }
        wait(countDown = e, timeout = 10)
    }

}
