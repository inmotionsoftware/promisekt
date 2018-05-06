package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenFulfilled
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch

class WhenConcurrentTests: AsyncTests() {

    @Test
    fun testWhen() {
        val e = CountDownLatch(1)

        val numbers = (0..41)
        val squareNumbers = numbers.map { it * it }

        val thenables = object: Iterable<Thenable<Int>> {
            val numberIterator = numbers.iterator()

            override fun iterator(): Iterator<Thenable<Int>> {
                return object: Iterator<Thenable<Int>> {
                    override fun next(): Thenable<Int> {
                        val number = numberIterator.next()
                        return after(0.001).map { number * number }
                    }

                    override fun hasNext(): Boolean {
                        return numberIterator.hasNext()
                    }
                }
            }
        }
        whenFulfilled(thenables, concurrently = 5).done {
            assert(it.equals(squareNumbers))
        }.catch {
            fail(it.localizedMessage)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 3)
    }

    @Test
    fun testWhenEmptyGenerator() {
        val e = CountDownLatch(1)

        whenFulfilled(arrayListOf<Thenable<Int>>(), concurrently = 5).done { numbers ->
            assertEquals(0, numbers.count())
        }.catch {
            fail(it.localizedMessage)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

}