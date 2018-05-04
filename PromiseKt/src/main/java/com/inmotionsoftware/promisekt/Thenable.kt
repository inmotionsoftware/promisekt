package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.com.inmotionsoftware.promisekt.features.whenFulfilled
import java.util.concurrent.Executor

interface Thenable<T> {
    fun pipe(to: (Result<T>) -> Unit)
    val result: Result<T>?
}

fun <T, U: Thenable<T>> Thenable<T>.then(on: Executor? = conf.Q.map, body: (T) -> U): Promise<T> {
    val rp = Promise<T>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        val rv = body(it.value)
                        if (rv === rp) {
                            throw PMKError.returnedSelf()
                        }
                        rv.pipe(to = rp.box::seal)
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <T, U> Thenable<T>.map(on: Executor? = conf.Q.map, transform: (T) -> U): Promise<U> {
    val rp = Promise<U>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        rp.box.seal(Result.fulfilled(transform(it.value)))
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <T, U> Thenable<T>.compactMap(on: Executor? = conf.Q.map, transform: (T) -> U?): Promise<U> {
    val rp = Promise<U>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        val rv = try { transform(it.value) } catch (e: Exception) { null }
                        if (rv != null) {
                            rp.box.seal(Result.fulfilled(rv))
                        } else {
                            throw PMKError.compactMap()
                        }
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <T> Thenable<T>.done(on: Executor? = conf.Q.`return`, body: (T) -> Unit): Promise<Unit> {
    val rp = Promise<Unit>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        body(it.value)
                        rp.box.seal(Result.fulfilled(Unit))
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <T> Thenable<T>.get(on: Executor? = conf.Q.`return`, body: (T) -> Unit): Promise<T> {
    return map(on = on) {
        body(it)
        it
    }
}

fun <T> Thenable<T>.asVoid(): Promise<Unit> {
    return map(on = null) { }
}

val <T> Thenable<T>.error: Throwable? get() {
    val r = result
    return when (r) {
        is Result.rejected -> r.error
        else -> null
    }
}

val <T> Thenable<T>.isPending: Boolean get() {
    return result == null
}

val <T> Thenable<T>.isResolved: Boolean get() {
    return !isPending
}

val <T> Thenable<T>.isFulfilled: Boolean get() {
    return value != null
}

val <T> Thenable<T>.isRejected: Boolean get() {
    return error != null
}

val <T> Thenable<T>.value: T? get() {
    val r = result
    return when (r) {
        is Result.fulfilled -> r.value
        else -> null
    }
}

// Thenable where T: Iterable

fun <E, T: Iterable<E>, U> Thenable<T>.mapValues(on: Executor? = conf.Q.map, transform: (E) -> U): Promise<Iterable<U>> {
    return map(on = on){ it.map(transform) }
}

fun <E, T: Iterable<E>, U> Thenable<T>.flatMapValues(on: Executor? = conf.Q.map, transform: (E) -> U?): Promise<Iterable<U>> {
    return map(on = on) { foo ->
        foo.flatMap {
            val value = transform(it)
            if (value != null) listOf(value) else listOf()
        }
    }
}

fun <E, T: Iterable<E>, U> Thenable<T>.compactMapValues(on: Executor? = conf.Q.map, transform: (E) -> U?): Promise<Iterable<U>> {
    return map(on = on) { foo ->
        foo.flatMap {
            val value = transform(it)
            if (value != null) listOf(value) else listOf()
        }
    }
}

fun <E, T: Iterable<E>, U, TU: Thenable<U>> Thenable<T>.thenMap(on: Executor? = conf.Q.map, transform: (E) -> TU): Promise<Iterable<U>> {
    val rp = Promise<Iterable<U>>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        val rv = whenFulfilled(it.value.map(transform))
                        rv.pipe(to = rp.box::seal)
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <E, T: Iterable<E>, UE, U: Iterable<UE>, TU: Thenable<U>> Thenable<T>.thenFlatMap(on: Executor? = conf.Q.map, transform: (E) -> TU): Promise<Iterable<UE>> {
    val rp = Promise<Iterable<UE>>(PMKUnambiguousInitializer.pending)
    pipe {
        when (it) {
            is Result.fulfilled -> {
                on.async {
                    try {
                        val rv = whenFulfilled(it.value.map(transform)).map(on = null) { it.flatMap { it }.asIterable() }
                        rv.pipe(to = rp.box::seal)
                    } catch (e: Throwable) {
                        rp.box.seal(Result.rejected(e))
                    }
                }
            }
            is Result.rejected -> {
                rp.box.seal(Result.rejected(it.error))
            }
        }
    }
    return rp
}

fun <E, T: Iterable<E>> Thenable<T>.filterValues(on: Executor? = conf.Q.map, isIncluded: (E) -> Boolean): Promise<Iterable<E>> {
    return map(on = on) { it.filter(isIncluded)}
}

val <E, T: Iterable<E>> Thenable<T>.firstValue: Promise<E>
    get() {
    return map(on = null) { aa ->
        try {
            aa.first()
        } catch (e: NoSuchElementException) {
            throw PMKError.emptySequence()
        }
    }
}

val <E, T: Iterable<E>> Thenable<T>.lastValue: Promise<E>
    get() {
    return map(on = null) { aa ->
        try {
            aa.last()
        } catch (e: NoSuchElementException) {
            throw PMKError.emptySequence()
        }
    }
}

fun <E: Comparable<E>, T: Iterable<E>> Thenable<T>.sortedValues(on: Executor? = conf.Q.map, selector: (E) -> E?): Promise<Iterable<E>> {
    return map(on = on) { it.sortedBy(selector) }
}
