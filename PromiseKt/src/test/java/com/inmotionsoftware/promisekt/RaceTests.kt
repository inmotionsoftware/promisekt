package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.features.race
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RaceTests: AsyncTests() {

    @Test
    fun test1() {
        val e = CountDownLatch(1)
        race(after(0.01).thenPromise { Promise.value(1) }, after(1.0).map { 2 }).done { index ->
            assertEquals(1, index)
            e.countDown()
        }.catch {
            fail(it.localizedMessage)
        }
        wait(countDown = e, timeout = 2)
    }

    @Test
    fun test2() {
        val e = CountDownLatch(1)
        race(after(1.0).map { 1 }, after(0.01).map { 2 }).done { index ->
            assertEquals(2, index)
            e.countDown()
        }
        wait(countDown = e, timeout = 2)
    }

    @Test
    fun test1Array() {
        val e = CountDownLatch(1)
        val promises = arrayListOf(after(.01).map{ 1 }, after(1.0).map{ 2 })
        race(promises).done { index ->
            assertEquals(1, index)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun test2Array() {
        val e = CountDownLatch(1)
        race(after(1.0).map { 1 }, after(.01).map { 2 }).done { index ->
            assertEquals(2, index)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testEmptyArray() {
        val e = CountDownLatch(1)
        val empty: Iterable<Promise<Int>> = arrayListOf()
        race(empty).catch {
            if (it !is PMKError.badInput) { fail() }
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

}