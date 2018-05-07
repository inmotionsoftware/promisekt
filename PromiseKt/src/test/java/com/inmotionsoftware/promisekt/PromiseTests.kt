package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.features.firstly
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch

class PromiseTests: AsyncTests() {

    sealed class LocalError: Throwable() {
        class dummy: LocalError()
    }

    @Test
    fun testIsPending() {
        assertTrue(Promise.pending<Unit>().first.isPending)
        assertFalse(Promise.value(Unit).isPending)
        assertFalse(Promise<Unit>(error = LocalError.dummy()).isPending)
    }

    @Test
    fun testIsResolved() {
        assertFalse(Promise.pending<Unit>().first.isResolved)
        assertTrue(Promise.value(Unit).isResolved)
        assertTrue(Promise<Unit>(error = LocalError.dummy()).isResolved)
    }

    @Test
    fun testIsFulfilled() {
        assertFalse(Promise.pending<Unit>().first.isFulfilled)
        assertTrue(Promise.value(Unit).isFulfilled)
        assertFalse(Promise<Unit>(error = LocalError.dummy()).isFulfilled)
    }

    @Test
    fun testIsRejected() {
        assertFalse(Promise.pending<Unit>().first.isRejected)
        assertTrue(Promise<Unit>(error = LocalError.dummy()).isRejected)
        assertFalse(Promise.value(Unit).isRejected)
    }

    @Test
    fun testDispatchQueueAsyncExtensionReturnsPromise() {
        val e = CountDownLatch(1)

        DispatchExecutor.global.async(PMKNamespacer.promise) {
            1
        }.done { one ->
            assertEquals(1, one)
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testDispatchQueueAsyncExtensionCanThrowInBody() {
        val e = CountDownLatch(1)

        DispatchExecutor.global.async(PMKNamespacer.promise) {
            throw LocalError.dummy()
        }.done {
            fail()
        }.catch {
            e.countDown()
        }
        wait(countDown = e, timeout = 1)
    }

    @Test
    fun testCannotFulfillWithError() {

        // sadly this test proves the opposite :(
        // left here so maybe one day we can prevent instantiation of `Promise<Error>`

        Promise<Unit> { seal ->
            seal.reject(LocalError.dummy())
        }

        Promise.pending<LocalError>()

        Promise.value(LocalError.dummy())

        Promise.value(Unit).map { LocalError.dummy() }
    }

    @Test
    fun testCanMakeVoidPromise() {
        Promise.value(Unit)
        Guarantee.value(Unit)
    }

    @Test
    fun testThrowInInitializer() {
        val p = Promise<Void> {
            throw LocalError.dummy()
        }
        assertTrue(p.isRejected)
        if (p.error !is LocalError.dummy) fail()
    }

    @Test
    fun testThrowInFirstly() {
        val e = CountDownLatch(1)

        firstly {
            throw LocalError.dummy()
            Promise.value(Unit)
        }.catch {
            assertTrue(it is LocalError.dummy)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

    @Test
    fun testWait() {
        val p = after(0.1).thenPromise(on = null){ Promise.value(1) }
        assertEquals(try { p.wait() } catch(e: Throwable) {}, 1)

        try {
            val p1 = after(0.1).mapPromise (on = null){ throw LocalError.dummy() }
            p1.wait()
        } catch(e: Throwable) {
            assertTrue(e is LocalError.dummy)
        }
    }

    @Test
    fun testPipeForResolved() {
        val e = CountDownLatch(1)
        Promise.value(1).done {
            assertEquals(1, it)
            e.countDown()
        }
        wait(countDown = e, timeout = 10)
    }

}
