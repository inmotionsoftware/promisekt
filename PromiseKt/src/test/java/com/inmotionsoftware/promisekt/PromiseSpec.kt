package com.inmotionsoftware.promisekt

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.assertEquals
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class PromiseSpec: Spek({
    given("a value = 2") {
        val value = 2
        on("immediately resolved"){
            Promise.value(value)
                    .done { assertEquals(value, it) }
                    .catch { assert(false) }
        }
        on("immediately resolved then multiply by 2") {
            Promise.value(value)
                    .then { Promise.value(it * 2) }
                    .done { assertEquals(4, it) }
                    .catch { assert(false)  }
        }
        on("immediately resolved then multiply by 2 then divide by 2") {
            Promise.value(value)
                    .then { Promise.value(it * 2) }
                    .then { Promise.value(it / 2) }
                    .done { assertEquals(value, it) }
                    .catch { assert(false) }
        }
    }
})