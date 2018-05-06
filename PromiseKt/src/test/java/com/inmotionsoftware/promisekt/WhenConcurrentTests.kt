package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.features.whenFulfilled
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.math.max

class WhenConcurrentTests: AsyncTests() {
    sealed class LocalError: Throwable() {
        class dummy: LocalError()
        class divisionByZero: LocalError()
    }

    @Test
    fun testWhen() {
        val e = CountDownLatch(1)

        val numbers = (0..42-1)
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
            println(it.localizedMessage)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testWhenGeneratorError() {
        val e = CountDownLatch(1)

        val expectedErrorIndex = 42
        val expectedError = LocalError.divisionByZero()
        val numbers = (-expectedErrorIndex..expectedErrorIndex-1)

        val thenables = object: Iterable<Thenable<Int>> {
            val numberIterator = numbers.iterator()

            override fun iterator(): Iterator<Thenable<Int>> {
                return object: Iterator<Thenable<Int>> {
                    override fun next(): Thenable<Int> {
                        val number = numberIterator.next()
                        return after(0.01).thenPromise {
                            if (number != 0) Promise<Int>(error = expectedError) else Promise.value(100500 / number)
                        }
                    }

                    override fun hasNext(): Boolean {
                        return numberIterator.hasNext()
                    }
                }
            }
        }

        whenFulfilled(thenables, concurrently = 3)
        .catch { error ->
            assert(error is LocalError)

            when (error) {
                is LocalError.divisionByZero -> { }
                else -> { assert(false) }
            }
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 3)
    }

    @Test
    fun testWhenConcurrency() {
        val e = CountDownLatch(1)

        var currentConcurrently = 0
        var maxConcurrently = 0
        val expectedConcurrently = 4

        val numbers = (0..42-1)

        val thenables = object: Iterable<Thenable<Int>> {
            val numberIterator = numbers.iterator()

            override fun iterator(): Iterator<Thenable<Int>> {
                return object: Iterator<Thenable<Int>> {
                    override fun next(): Thenable<Int> {
                        currentConcurrently += 1
                        maxConcurrently = max(maxConcurrently, currentConcurrently)

                        val number = numberIterator.next()
                        return after(0.01).thenPromise(on = DispatchExecutor.main) {
                            currentConcurrently -= 1
                            Promise.value(number * number)
                        }
                    }

                    override fun hasNext(): Boolean {
                        return numberIterator.hasNext()
                    }
                }
            }
        }

        whenFulfilled(thenables, concurrently = expectedConcurrently).done {
            assertEquals(expectedConcurrently, maxConcurrently)
        }.catch {
            fail(it.localizedMessage)
        }.finally {
            e.countDown()
        }

        wait(countDown = e, timeout = 3)
    }

    @Test
    fun testWhenConcurrencyLessThanZero() {
        val thenables = object: Iterable<Thenable<Int>> {
            override fun iterator(): Iterator<Thenable<Int>> {
                return object: Iterator<Thenable<Int>> {
                    override fun next(): Thenable<Int> {
                        fail()
                        return Promise.value(0)
                    }

                    override fun hasNext(): Boolean {
                        fail()
                        return false
                    }
                }
            }
        }

        val p1 = whenFulfilled(thenables, concurrently = 0)
        val p2 = whenFulfilled(thenables, concurrently = -1)

        if (p1.error == null) { return fail() }
        if (p2.error == null) { return fail() }
        if (p1.error !is PMKError.badInput) { return fail() }
        if (p2.error !is PMKError.badInput) { return fail() }
    }

    @Test
    fun testStopsDequeueingOnceRejected() {
        val e = CountDownLatch(1)
        var x: Int = 0

        val thenables = object: Iterable<Thenable<Unit>> {
            override fun iterator(): Iterator<Thenable<Unit>> {
                return object: Iterator<Thenable<Unit>> {
                    override fun next(): Thenable<Unit> {
                        x += 1
                        return when (x) {
                            0 -> {
                                fail("Fatal")
                                Promise.value(Unit)
                            }
                            1 -> { Promise.value(Unit) }
                            2 -> { Promise(error = LocalError.dummy()) }
                            else -> {
                                fail()
                                Promise.value(Unit)
                            }
                        }
                    }

                    override fun hasNext(): Boolean {
                        return true
                    }
                }
            }
        }

        whenFulfilled(thenables, concurrently = 1).done {
            fail()
        }.catch {
            e.countDown()
        }
        wait(countDown = e, timeout = 3)
    }

}