package com.inmotionsoftware.promisekt

import org.junit.Assert.assertEquals
import org.junit.Test

class ThenableTests {
    sealed class E: Throwable() {
        class dummy: E()
    }

    @Test
    fun testGet() {
        Promise.value(1).get {
            assertEquals(it, 1)
        }.done {
            assertEquals(it, 1)
        }.catch {
            assert(false) { it.localizedMessage }
        }
    }

    @Test
    fun testCompactMap() {
        Promise.value(1.0).compactMap {
            it.toInt()
        }.done {
            assertEquals(it, 1)
        }.catch {
            assert(false) { it.localizedMessage }
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
                    assert(false) { it.localizedMessage }
                }
            }
        }
    }

    @Test
    fun testRejectedPromiseCompactMap() {
        Promise.value("a").compactMap {
            it.toInt()
        }.catch {
            when (it) {
                is PMKError.compactMap -> {}
                else -> {
                    assert(false) { it.localizedMessage }
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
            assert(false) { it.localizedMessage }
        }
    }
}