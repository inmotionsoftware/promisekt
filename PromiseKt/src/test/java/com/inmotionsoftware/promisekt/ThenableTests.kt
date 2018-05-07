package com.inmotionsoftware.promisekt

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch

class ThenableTests: AsyncTests() {
    sealed class E: Throwable() {
        class dummy: E()
    }

    @Test
    fun testGet() {
        val e = CountDownLatch(2)
        Promise.value(1).get {
            assertEquals(1, it)
            e.countDown()
        }.done {
            assertEquals(1, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testCompactMap() {
        val e = CountDownLatch(1)
        Promise.value(1.0).compactMap {
            it.toInt()
        }.done {
            assertEquals(1, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testCompactMapThrows() {
        val e = CountDownLatch(1)
        Promise.value("a").compactMap {
            throw E.dummy()
        }.catch {
            when (it) {
                is E.dummy -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testRejectedPromiseCompactMap() {
        val e = CountDownLatch(1)
        Promise<String>(error = E.dummy()).compactMap {
            it.toInt()
        }.catch {
            when (it) {
                is E.dummy -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testPMKErrorCompactMap() {
        val e = CountDownLatch(1)
        Promise.value("a").compactMap {
            it.toInt()
        }.catch {
            when (it) {
                is PMKError.compactMap -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testMapValues() {
        val e = CountDownLatch(1)
        Promise.value(arrayListOf("1", "2", "3", "4")).mapValues { value ->
            value.toInt()
        }.done {
            assertEquals(arrayListOf(1, 2, 3, 4), it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testFlatMapValues() {
        val e = CountDownLatch(1)
        Promise.value(arrayListOf(1, 2, 3, 4)).flatMapValues {
            arrayListOf(it, it)
        }.done {
            assertEquals(arrayListOf(1, 1, 2, 2, 3, 3, 4, 4), it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testCompactMapValues() {
        val e = CountDownLatch(1)
        Promise.value(arrayListOf("1", "2", "a", "4")).compactMapValues { value ->
            try { value.toInt() } catch (e: Exception) { null }
        }.done {
            assertEquals(arrayListOf(1, 2, 4), it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testThenMap() {
        val e = CountDownLatch(1)
        Promise.value(arrayListOf(1, 2, 3, 4)).thenMap {
            Promise.value(it * 2)
        }.done {
            assertEquals(arrayListOf(2, 4, 6, 8), it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testThenFlatMap() {
        val e = CountDownLatch(1)
        Promise.value(arrayListOf(1,2,3,4)).thenFlatMap {
            Promise.value(arrayListOf(it, it))
        }.done {
            assertEquals(arrayListOf(1, 1, 2, 2, 3, 3, 4, 4), it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testLastValueForEmpty() {
        assert(Promise.value(emptyList<Int>()).lastValue.isRejected)
    }

    @Test
    fun testFirstValueForEmpty() {
        assert(Promise.value(emptyList<Int>()).firstValue.isRejected)
    }

    @Test
    fun testThenOffRejected() {
        val e = CountDownLatch(1)
        Promise<Int>(error = PMKError.badInput()).then {
            fail()
            Promise.value(it)
        }.catch {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

}
