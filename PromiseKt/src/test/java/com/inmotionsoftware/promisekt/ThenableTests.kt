package com.inmotionsoftware.promisekt

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ThenableTests {
    sealed class E: Throwable() {
        class dummy: E()
    }

    @Test
    fun testGet() {
        Promise.value(1).get {
            assertEquals(1, it)
        }.done {
            assertEquals(1, it)
        }.catch {
            fail(it.localizedMessage)
        }
    }

    @Test
    fun testCompactMap() {
        Promise.value(1.0).compactMap {
            it.toInt()
        }.done {
            assertEquals(1, it)
        }.catch {
            fail(it.localizedMessage)
        }
    }

    @Test
    fun testCompactMapThrows() {
        Promise.value("a").compactMap {
            throw E.dummy()
        }.catch {
            when (it) {
                is E.dummy -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
        }
    }

    @Test
    fun testRejectedPromiseCompactMap() {
        Promise<String>(error = E.dummy()).compactMap {
            it.toInt()
        }.catch {
            when (it) {
                is E.dummy -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
        }
    }

    @Test
    fun testPMKErrorCompactMap() {
        Promise.value("a").compactMap {
            it.toInt()
        }.catch {
            when (it) {
                is PMKError.compactMap -> {}
                else -> {
                    fail(it.localizedMessage)
                }
            }
        }
    }

    @Test
    fun testCompactMapValues() {
        Promise.value(arrayListOf("1", "2", "a", "4")).compactMapValues { value ->
            try { value.toInt() } catch (e: Exception) { null }
        }.done {
            assertEquals(it, arrayListOf("1", "2", "4"))
        }.catch {
            fail(it.localizedMessage)
        }
    }

//    @Test
//    fun testThenMap() {
//        Promise.value(arrayListOf(1,2,3,4)).thenMap {
//            Promise.value(it)
//        }.done {
//            assertEquals(arrayListOf(1,2,3,4), it)
//        }.catch {
//            assert(false) { it.localizeMessage }
//        }
//    }

//    @Test
//    fun testThenFlatMap() {
//        Promise.value(arrayListOf(1,2,3,4)).thenFlatMap {
//            Promise.value(arrayListOf(it, it))
//        }.done {
//            assertEquals(arrayListOf(1,1,2,2,3,3,4,4), it)
//        }.catch {
//            assert(false) { it.localizeMessage }
//        }
//    }

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
        Promise<Int>(error = PMKError.badInput()).then {
            fail()
            Promise.value(it)
        }.catch { }
    }
}
