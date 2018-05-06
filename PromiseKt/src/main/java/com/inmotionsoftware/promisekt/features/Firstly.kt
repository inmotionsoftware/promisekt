package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.PMKUnambiguousInitializer
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Thenable

fun <T, U: Thenable<T>> firstly(body: () -> U): Promise<T> {
    try {
        val rp = Promise<T>(PMKUnambiguousInitializer.pending)
        body().pipe(to = rp.box::seal )
        return rp
    } catch (e: Throwable) {
        return Promise(error = e)
    }
}

fun <T> firstly(body: () -> Guarantee<T>): Guarantee<T> {
    return body()
}
