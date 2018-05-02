package com.inmotionsoftware.promisekt

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CatchableTests {

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
            var x = 0
            Promise<Unit>(error = error).catch(policy = CatchPolicy.allErrors) {
                assertEquals(0, x)
                x += 1
            }.finally {
                assertEquals(1, x)
                x += 1
            }
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun testCauterize() {
        val p = Promise<Unit>(error = E.dummy())
        p.cauterize()
        p.catch { }
    }

    //
    // Promise<Unit>.recover
    //

    @Test
    fun test__void_specialized_full_recover() {
        val helper: (Throwable) -> Unit = { error ->
            val x = 0
            Promise<Int>(error = error).guaranteeRecover {
                Guarantee.value(x+1)
            }.done {
                assertEquals(1, it)
            }
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun test__void_specialized_full_recover__fulfilled_path() {
        val x = 0
        Promise.value(x).recover {
            fail()
            Guarantee.value(1)
        }.done {
            assertEquals(x, it)
        }
    }

    @Test
    fun test__void_specialized_conditional_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            var x = 0
            Promise<Unit>(error = error).recover(policy = policy) { err ->
                if (x >= 1) throw err
                x += 1
                Guarantee.value(Unit)
            }.done {
                assertEquals(1, it)
            }
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__void_specialized_conditional_recover__no_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            Promise<Unit>(error = error).recover(policy = policy) { err ->
                throw err
            }.catch(policy = CatchPolicy.allErrors) {
                assertEquals(error, it)
            }
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__void_specialized_conditional_recover__ignores_cancellation_but_fed_cancellation() {
        val err = E.cancelled()
        Promise<Unit>(error = err).recover(policy = CatchPolicy.allErrorsExceptCancellation) {
            fail()
            Guarantee.value(Unit)
        }.catch(policy = CatchPolicy.allErrors) {
            assertEquals(err, it)
        }
    }

    @Test
    fun test__void_specialized_conditional_recover__fulfilled_path() {
        Promise.value(Unit).recover {
            fail()
            Guarantee.value(Unit)
        }.catch {
            fail()
        }.finally {
            assert(true)
        }
    }

    //
    // Promise<T>.recover
    //

    @Test
    fun test__full_recover() {
        val helper: (Throwable) -> Unit = { error ->
            Promise<Int>(error = error).recover { Guarantee.value(2) }.done {
                assertEquals(2, it)
            }
        }
        helper(E.dummy())
        helper(E.cancelled())
    }

    @Test
    fun test__full_recover__fulfilled_path() {
        Promise.value(1).recover {
            fail()
            Guarantee.value(2)
        }.done {
            assertEquals(1, it)
        }
    }

    @Test
    fun test__conditional_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            var x = 0
            Promise<Int>(error = error).recover(policy = policy) { err ->
                if (x >= 1) throw err
                x += 1
                Promise.value(x)
            }.done {
                assertEquals(x, it)
            }
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__conditional_recover__no_recover() {
        val helper: (CatchPolicy, Throwable) -> Unit = { policy, error ->
            Promise<Int>(error = error).recover(policy = policy) { err ->
                throw err
            }.catch(policy = CatchPolicy.allErrors) {
                assertEquals(error, it)
            }
        }
        arrayListOf(E.dummy(), E.cancelled()).forEach {
            helper(CatchPolicy.allErrors, it)
        }
        helper(CatchPolicy.allErrorsExceptCancellation, E.dummy())
    }

    @Test
    fun test__conditional_recover__ignores_cancellation_but_fed_cancellation() {
        val error = E.cancelled()
        Promise<Int>(error = error).recover(policy = CatchPolicy.allErrorsExceptCancellation) {
            fail()
            Guarantee.value(1)
        }.catch(policy = CatchPolicy.allErrors) {
            assertEquals(error, it)
        }
    }

    @Test
    fun test__conditional_recover__fulfilled_path() {
        Promise.value(1).recover { err ->
            fail()
            throw err
        }.done {
            assertEquals(1, it)
        }.catch {
            fail()
        }
    }

//    @Test
//    fun testEnsureThen_Error() {
//        Promise.value(1).done {
//            assertEquals(1, it)
//            throw E.dummy()
//        }.ensureThen {
//            after(seconds: 0.01)
//        }.catch {
////            assertEquals(Error.dummy, $0 as? Error)
//        }.finally {
//        }
//    }
//
//    @Test
//    fun testEnsureThen_Value() {
//        Promise.value(1).ensureThen {
//            after(seconds: 0.01)
//        }.done {
//            assertEquals(1, it)
//        }.catch {
//            fail()
//        }.finally {
//        }
//    }

}