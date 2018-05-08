package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.*

/**
 * Resolves with the first resolving promise from a set of promises.
 *
 *   race(promise1, promise2, promise3).then { winner ->
 *      //...
 *   }
 *
 * @return: A new promise that resolves when the first promise in the provided promises resolves.
 * Warning: If any of the provided promises reject, the returned promise is rejected.
 * Remark: Returns promise rejected with PMKError.badInput if empty list provided
 */
fun <T, U: Thenable<T>> race(vararg thenables: U): Promise<T> {
    return race(thenables.asIterable())
}

/**
 * Resolves with the first resolving promise from a set of promises.
 *
 *   race(promise1, promise2, promise3).then { winner ->
 *      //...
 *   }
 *
 * @return: A new promise that resolves when the first promise in the provided promises resolves.
 * Warning: If any of the provided promises reject, the returned promise is rejected.
 * Remark: Returns promise rejected with PMKError.badInput if empty list provided
 */
fun <T, U: Thenable<T>> race(thenables: Iterable<U>): Promise<T> {
    if (thenables.count() == 0) return Promise(error = PMKError.badInput())

    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    thenables.forEach {
        it.pipe(to = rp.box::seal)
    }
    return rp
}

/**
 * Resolves with the first resolving Guarantee from a set of guarantees.
 *
 *   race(guarantee1, guarantee2, guarantee3).then { winner ->
 *      //…
 *   }
 *
 * @return: A new guarantee that resolves when the first guaranteed in the provided guarantees resolves.
 */
fun <T, U: Guarantee<T>> raceGuarantee(vararg guarantees: U): Guarantee<T> {
    return raceGuarantee(guarantees.asIterable())
}

/**
 * Resolves with the first resolving Guarantee from a set of guarantees.
 *
 *   race(guarantee1, guarantee2, guarantee3).then { winner ->
 *      //…
 *   }
 *
 * @return: A new guarantee that resolves when the first guaranteed in the provided guarantees resolves.
 */
fun <T, U: Guarantee<T>> raceGuarantee(guarantees: Iterable<U>): Guarantee<T> {
    val rg = Guarantee<T>(PMKUnambiguousInitializer.pending)
    guarantees.forEach {
        it.pipeTo(to = rg.box::seal)
    }
    return rg
}
