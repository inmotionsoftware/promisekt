package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenResolved
import org.junit.Test

class WhenResolvedTests: AsyncTests() {

    @Test
    fun testImmediates() {
        val successPromise = Promise.value(Unit)

        var joinFinished = false
        whenResolved(successPromise).done(on = null) { joinFinished = true }
        assert(joinFinished) { "Join immediately finishes on fulfilled promise" }

        val promise2 = Promise.value(2)
        val promise3 = Promise.value(3)
        val promise4 = Promise.value(4)
        var join2Finished = false
        whenResolved(promise2, promise3, promise4).done(on = null) { join2Finished = true }
        assert(join2Finished) { "Join immediately finishes on fulfilled promises" }
    }

    @Test
    fun testFulfilledAfterAllResolve() {
        val (promise1, seal1) = Promise.pending<Unit>()
        val (promise2, seal2) = Promise.pending<Unit>()
        val (promise3, seal3) = Promise.pending<Unit>()

        var finished = false
        whenResolved(promise1, promise2, promise3).done(on = null) { finished = true }
        assert(!finished) { "Not all promises have resolved" }

        seal1.fulfill(Unit)
        assert(!finished){ "Not all promises have resolved" }

        seal2.fulfill(Unit)
        assert(!finished){ "Not all promises have resolved" }

        seal3.fulfill(Unit)
        assert(finished) { "All promises have resolved" }
    }

}
