package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.PMKUnambiguousInitializer
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Thenable

/**
 * Judicious use of `firstly` *may* make chains more readable.
 *
 * Compare:
 *
 *   authenticateUser(username, passowrd).then {
 *      validateToken(it)
 *   }.then {
 *      getUserProfile()
 *   }
 *
 * With:
 *
 *   firstly {
 *      authicateUser(username, password)
 *   }.then {
 *      validateToken(it)
 *   }.then {
 *     getUserProfile()
 *   }
 *
 * Note: the block you pass excecutes immediately on the current thread/queue.
 */
fun <T, U: Thenable<T>> firstly(body: () -> U): Promise<T> {
    return try {
        val rp = Promise<T>(PMKUnambiguousInitializer.pending)
        body().pipe(to = rp.box::seal )
        rp
    } catch (e: Throwable) {
        Promise(error = e)
    }
}

/**
 * Same as `firstly` but resolves in a Guarantee.
 */
fun <T, U: Guarantee<T>> firstlyGuarantee(body: () -> U): Guarantee<T> {
    return body()
}
