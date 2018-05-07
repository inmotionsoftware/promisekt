package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch

class CatchableTests: AsyncTests() {

    private sealed class E: Throwable(), CancellableError {
        class dummy: E()
        class cancelled: E()

        override val isCancelled: Boolean get() {
            return when (this) {
                is E.cancelled -> true
                else -> false
            }
        }
    }

    @Test
    fun testFinally() {
        val helper: (Throwable) -> Unit = { error ->
            val e = CountDownLatch(2)
            var x = 0
            Promise<Unit>(error = error).catch(policy = CatchPolicy.allErrors) {
                assertEquals(0, x)
                x += 1
                e.countDown()
            }.finally {
                assertEquals(1, x)
                e.countDown()
            }
            wait(countDown = e, timeout = 10)
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun testCauterize() {
        val e = CountDownLatch(1)
        val p = Promise<Unit>(error = E.dummy())
        p.cauterize()
        p.catch {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    //
    // Promise<Unit>.recover
    //

    @Test
    fun test__void_specialized_full_recover() {
        val helper: (Throwable) -> Unit = { error ->
            val e = CountDownLatch(1)
            Promise<Unit>(error = error).guaranteeRecover { Guarantee.value(Unit) }.done { e.countDown() }
            wait(countDown = e, timeout = 10)
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun test__void_specialized_full_recover__fulfilled_path() {
        val e = CountDownLatch(1)
        Promise.value(Unit).recover {
            fail()
            Guarantee.value(Unit)
        }.done {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun test__void_specialized_conditional_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            val e = CountDownLatch(1)
            var x = 0
            Promise<Unit>(error = error).recover(policy = policy) { err ->
                if (x >= 1) throw err
                x += 1
                Guarantee.value(Unit)
            }.done {
                e.countDown()
            }
            wait(countDown = e, timeout = 10)
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__void_specialized_conditional_recover__no_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            val e = CountDownLatch(1)
            Promise<Unit>(error = error).recover(policy = policy) { err ->
                throw err
            }.catch(policy = CatchPolicy.allErrors) {
                assertEquals(error, it)
                e.countDown()
            }
            wait(countDown = e, timeout = 10)
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__void_specialized_conditional_recover__ignores_cancellation_but_fed_cancellation() {
        val e = CountDownLatch(1)
        Promise<Unit>(error = E.cancelled()).recover(policy = CatchPolicy.allErrorsExceptCancellation) {
            fail()
            Guarantee.value(Unit)
        }.catch(policy = CatchPolicy.allErrors) {
            assert(it is E.cancelled)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun test__void_specialized_conditional_recover__fulfilled_path() {
        val e = CountDownLatch(1)
        Promise.value(Unit).recover {
            fail()
            Guarantee.value(Unit)
        }.catch {
            fail()
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    //
    // Promise<T>.recover
    //

    @Test
    fun test__full_recover() {
        val helper: (Throwable) -> Unit = { error ->
            val e = CountDownLatch(1)
            Promise<Int>(error = error).guaranteeRecover {
                Guarantee.value(2)
            }.done {
                assertEquals(2, it)
                e.countDown()
            }
            wait(countDown = e, timeout = 2)
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun test__full_recover__fulfilled_path() {
        val e = CountDownLatch(1)
        Promise.value(1).recover {
            fail()
            Guarantee.value(2)
        }.done {
            assertEquals(1, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun test__conditional_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            val e = CountDownLatch(1)
            var x = 0
            Promise<Int>(error = error).recover(policy = policy) { err ->
                if (x >= 1) throw err
                x += 1
                Promise.value(x)
            }.done {
                assertEquals(x, it)
                e.countDown()
            }
            wait(countDown = e, timeout = 10)
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__conditional_recover__no_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            val e = CountDownLatch(1)
            Promise<Int>(error = error).recover(policy = policy) { err ->
                throw err
            }.catch(policy = CatchPolicy.allErrors) {
                assertEquals(error, it)
                e.countDown()
            }
            wait(countDown = e, timeout = 10)
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__conditional_recover__ignores_cancellation_but_fed_cancellation() {
        val e = CountDownLatch(1)
        Promise<Int>(error = E.cancelled()).recover(policy = CatchPolicy.allErrorsExceptCancellation) {
            fail()
            Guarantee.value(1)
        }.catch(policy = CatchPolicy.allErrors) {
            assert(it is E.cancelled)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun test__conditional_recover__fulfilled_path() {
        val e = CountDownLatch(1)
        Promise.value(1).recover { err ->
            fail()
            throw err
        }.done {
            assertEquals(1, it)
            e.countDown()
        }.catch {
            fail()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testEnsure() {
        val e = CountDownLatch(1)
        Promise.value(1).done {
            assertEquals(1, it)
            throw E.dummy()
        }.ensure {
            after(seconds = 0.01)
        }.catch {
            assert(it is E.dummy)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testEnsureThen_Error() {
        val e = CountDownLatch(1)
        Promise.value(1).done {
            assertEquals(1, it)
            throw E.dummy()
        }.ensureThen {
            after(seconds = 0.01)
        }.catch {
            assert(it is E.dummy)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testEnsureThen_Value() {
        val e = CountDownLatch(1)
        Promise.value(1).ensureThen {
            after(seconds = 0.01)
        }.done {
            assertEquals(1, it)
        }.catch {
            fail()
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

}