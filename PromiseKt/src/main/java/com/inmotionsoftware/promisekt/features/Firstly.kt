package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.PMKUnambiguousInitializer
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Thenable

fun <T, U: Thenable<T>> firstly(body: () -> U): Promise<T> {
    return try {
        val rp = Promise<T>(PMKUnambiguousInitializer.pending)
        body().pipe(to = rp.box::seal )
        rp
    } catch (e: Throwable) {
        Promise(error = e)
    }
}

fun <T, U: Guarantee<T>> firstlyGuarantee(body: () -> U): Guarantee<T> {
    return body()
}
