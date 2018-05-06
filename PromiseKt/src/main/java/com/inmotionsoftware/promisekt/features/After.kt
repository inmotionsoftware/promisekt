package com.inmotionsoftware.promisekt.features

import com.inmotionsoftware.promisekt.DispatchExecutor
import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.asyncAfter

fun after(seconds: Double): Guarantee<Unit> {
    val g = Guarantee.pending<Unit>()
    DispatchExecutor.global.asyncAfter(seconds) {
        g.second(Unit)
    }
    return g.first
}
