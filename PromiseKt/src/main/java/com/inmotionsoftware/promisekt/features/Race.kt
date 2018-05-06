package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.*

private fun <T, U: Thenable<T>> _race(thenables: Iterable<U>): Promise<T> {
    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    thenables.forEach {
        it.pipe(to = rp.box::seal)
    }
    return rp
}

fun <T, U: Thenable<T>> race(vararg thenables: U): Promise<T> {
    return _race(thenables.asIterable())
}

fun <T, U: Thenable<T>> race(thenables: Iterable<U>): Promise<T> {
    return if (thenables.count() == 0) Promise(error = PMKError.badInput()) else _race(thenables.asIterable())
}

fun <T, U: Guarantee<T>> race(vararg guarantees: U): Guarantee<T> {
    val rg = Guarantee<T>(PMKUnambiguousInitializer.pending)
    guarantees.forEach {
        it.pipeTo(to = rg.box::seal)
    }
    return rg
}
