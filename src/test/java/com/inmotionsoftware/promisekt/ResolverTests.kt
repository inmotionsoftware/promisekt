package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch

class ResolverTests: AsyncTests() {

    private sealed class LocalError: Throwable() {
        class test: LocalError()
    }

    private class KittenFetcher(val value: Int?, val error: Throwable?) {

        fun fetchWithCompletionBlock(block: (Int?, Throwable?) -> Unit) {
            after(0.02).done {
                block(this.value, this.error)
            }
        }

        fun fetchWithCompletionBlock2(block: (Throwable?, Int?) -> Unit) {
            after(.02).done {
                block(this.error, this.value)
            }
        }

        fun fetchWithCompletionBlock3(block: (Result<Int>) -> Unit) {
            after(.02).done {
                block(Result.fulfilled(this.value!!))
            }
        }

        fun fetchWithCompletionBlock3_Error(block: (Result<Int>) -> Unit) {
            after(.02).done {
                block(Result.rejected(this.error ?: LocalError.test()))
            }
        }

        fun fetchWithCompletionBlock4(block: (Throwable?) -> Unit) {
            after(0.02).done {
                block(this.error)
            }
        }
    }

    @Test
    fun testSuccess() {
        val e = CountDownLatch(1)
        val kittenFetcher = KittenFetcher(value = 2, error = null)
        Promise<Int> { seal ->
            kittenFetcher.fetchWithCompletionBlock(seal::resolve)
        }.done {
            assertEquals(2, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testError() {
        val e = CountDownLatch(1)

        val kittenFetcher = KittenFetcher(value = null, error = LocalError.test())
        Promise<Int> { seal ->
            kittenFetcher.fetchWithCompletionBlock(seal::resolve)
        }.catch {
            assertTrue (it is LocalError.test)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testInvalidCallingConvention() {
        val e = CountDownLatch(1)

        val kittenFetcher = KittenFetcher(value = null, error = null)
        Promise<Int> { seal ->
            kittenFetcher.fetchWithCompletionBlock(seal::resolve)
        }.catch {
            assertTrue(it is PMKError.invalidCallingConvention)
        }.finally {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testInvertedCallingConvention() {
        val e = CountDownLatch(1)

        val kittenFetcher = KittenFetcher(value = 2, error = null)
        Promise<Int?> { seal ->
            kittenFetcher.fetchWithCompletionBlock2(seal::resolve)
        }.done {
            assertEquals(2, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testResult() {
        val e = CountDownLatch(2)
        val kf1 = KittenFetcher(value = 1, error = null)
        Promise<Int> { seal ->
            kf1.fetchWithCompletionBlock3(seal::resolve)
        }.done {
            assertEquals(1, it)
            e.countDown()
        }

        val kf2 = KittenFetcher(value = null, error = LocalError.test())
        Promise<Unit> { seal ->
            kf2.fetchWithCompletionBlock4(seal::resolve)
        }.catch {
            assertTrue(it is LocalError.test)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testVoidCompletionValue() {
        val e = CountDownLatch(2)
        val kf1 = KittenFetcher(value = null, error = null)
        Promise<Unit> { seal ->
            kf1.fetchWithCompletionBlock4(seal::resolve)
        }.done {
            e.countDown()
        }

        val kf2 = KittenFetcher(value = null, error = LocalError.test())
        Promise<Unit> { seal ->
            kf2.fetchWithCompletionBlock4(seal::resolve)
        }.catch {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testIsFulfilled() {
        assertTrue(Promise.value(Unit).result?.isFulfilled ?: false)
        assertFalse(Promise<Int>(error = LocalError.test()).result?.isFulfilled ?: true)
    }

}
