package com.inmotionsoftware.promisekt

import com.inmotionsoftware.promisekt.features.whenFulfilled
import java.util.concurrent.Executor

/**
 * Thenable represents an asynchronous operation that can be chained.
 */
interface Thenable<T> {
    fun pipe(to: (Result<T>) -> Unit)
    val result: Result<T>?
}

/**
 * The provided closure executes when this promise resolves.
 *
 * This allows chaining promises. The promise returned by the provided closure is resolved before the promise returned by this closure resolves.
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The closure that executes when this promise fulfills. It must return a promise.
 * @return: A new promise that resolves when the promise returned from the provided closure resolves. For example:
 *
 *   firstly {
 *      service.getData()
 *   }.then { data ->
 *      transform(data)
 *   }.done { transformation ->
 *      //…
 *   }
 */
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

/**
 * The provided closure is executed when this promise is resolved.
 *
 * This is like `then` but it requires the closure to return a non-promise.
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param transform: The closure that is executed when this Promise is fulfilled. It must return a non-promise.
 * @return A new promise that is resolved with the value returned from the provided closure. For example:
 *
 *   firstly {
 *      service.getData()
 *   }.map { data ->
 *      length(data)
 *   }.done { length ->
 *      //…
 *   }
 */
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

fun <T, U, TU: Thenable<U>> Thenable<T>.thenMap(on: Executor? = conf.Q.map, body: (T) -> TU): Promise<U> {
    val rp = Promise<U>(PMKUnambiguousInitializer.pending)
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

/**
 * The provided closure is executed when this promise is resolved.
 *
 * In your closure return an `Optional`, if you return `null` the resulting promise is rejected with `PMKError.compactMap`,
 * otherwise the promise is fulfilled with the unwrapped value.
 *
 *   firstly {
 *      service.getData()
 *   }.compactMap {
 *      serializeToJson(data)
 *   }.done {
 *      //…
 *   }.catch {
 *     // either `PMKError.compactMap` or a `JSONError`
 *   }
 */
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

/**
 * The provided closure is executed when this promise is resolved.
 *
 * @param on: The queue to which the provided closure dispatches.
 * @param body: The closure that is executed when this Promise is fulfilled.
 * @return A new promise fulfilled as `Unit`.
 *
 *   firstly {
 *      service.getData()
 *   }.done { data ->
 *     print(data)
 *   }
 */
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

/**
 * The provided closure is executed when this promise is resolved.
 *
 * This is like `done` but it returns the same value that the handler is fed.
 * `get` immutably accesses the fulfilled value; the returned Promise maintains that value.
 *
 * @param on: The executor to which the provided closure dispatches.
 * @param body: The closure that is executed when this Promise is fulfilled.
 * @return A new promise that is resolved with the value that the handler is fed. For example:
 *
 *   firstly {
 *      Promise.value(1)
 *   }.get { foo ->
 *      println(foo, " is 1")
 *   }.done { foo ->
 *      println(foo, " is 1")
 *   }.done { foo ->
 *      println(foo, " is Void")
 *   }
 */
inline fun <T> Thenable<T>.get(on: Executor? = conf.Q.`return`, crossinline body: (T) -> Unit): Promise<T> {
    return map(on = on) {
        body(it)
        it
    }
}

/**
 * Returns a new promise chained off this promise but with its value discarded.
 */
fun <T> Thenable<T>.asVoid(): Promise<Unit> {
    return map(on = null) { }
}

/**
 * Returns the error with which this promise was rejected; `nil` if this promise is not rejected.
 */
inline val <T> Thenable<T>.error: Throwable? get() {
    val r = result
    return when (r) {
        is Result.rejected -> r.error
        else -> null
    }
}

/**
 * Returns `true` if the promise has not yet resolved.
 */
inline val <T> Thenable<T>.isPending: Boolean get() {
    return result == null
}

/**
 * Returns `true` if the promise has resolved.
 */
inline val <T> Thenable<T>.isResolved: Boolean get() {
    return !isPending
}

/**
 * Returns `true` if the promise was fulfilled.
 */
inline val <T> Thenable<T>.isFulfilled: Boolean get() {
    return value != null
}

/**
 * Returns `true` if the promise was rejected.
 */
inline val <T> Thenable<T>.isRejected: Boolean get() {
    return error != null
}

/**
 * Returns the value with which this promise was fulfilled or `nil` if this promise is pending or rejected.
 */
inline val <T> Thenable<T>.value: T? get() {
    val r = result
    return when (r) {
        is Result.fulfilled -> r.value
        else -> null
    }
}

// Thenable where T: Iterable

/**
 * `Promise<Iterable<T>>` => `T` -> `U` => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf(1,2,3))
 *   }.mapValues { integer ->
 *      integer * 2
 *   }.done {
 *      // it => [2,4,6]
 *   }
 */
inline fun <E, T: Iterable<E>, U> Thenable<T>.mapValues(on: Executor? = conf.Q.map, crossinline transform: (E) -> U): Promise<Iterable<U>> {
    return map(on = on){ it.map(transform) }
}

/**
 * `Promise<Iterable<T>>` => `T` -> `Iterable<U>` => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf(1,2,3))
 *   }.flatMapValues { integer ->
 *      arrayListOf(integer, integer)
 *   }.done {
 *      // it => [1,1,2,2,3,3]
 *   }
 */
inline fun <E, T: Iterable<E>, UE, U: Iterable<UE>> Thenable<T>.flatMapValues(on: Executor? = conf.Q.map, crossinline transform: (E) -> U): Promise<Iterable<UE>> {
    return map(on = on) { foo ->
        foo.flatMap {
            transform(it).flatMap { listOf(it) }
        }
    }
}

/**
 * `Promise<Iterable<T>>` => `T` -> `U?` => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf("1","2","a","3"))
 *   }.compactMapValues {
 *      it.toInt()
 *   }.done {
 *      // it => [1,2,3]
 *   }
 */
inline fun <E, T: Iterable<E>, U> Thenable<T>.compactMapValues(on: Executor? = conf.Q.map, crossinline transform: (E) -> U?): Promise<Iterable<U>> {
    return map(on = on) { foo ->
        foo.flatMap {
            val value = transform(it)
            if (value != null) listOf(value) else listOf()
        }
    }
}

/**
 * `Promise<Iterable<T>>` => `T` -> `Promise<U>` => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf(1,2,3))
 *   }.thenMap { integer ->
 *      Promise.value(integer * 2)
 *   }.done {
 *      // it => [2,4,6]
 *   }
 */
fun <E, T: Iterable<E>, U, TU: Thenable<U>> Thenable<T>.thenMapValues(on: Executor? = conf.Q.map, transform: (E) -> TU): Promise<Iterable<U>> {
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

/**
 * `Promise<Iterable<T>>` => `T` -> `Promise<Iterable<U>>` => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf(1,2,3))
 *   }.thenFlatMap { integer ->
 *      Promise.value(arrayListOf(integer, integer))
 *   }.done {
 *      // it => [1,1,2,2,3,3]
 *   }
 */
fun <E, T: Iterable<E>, UE, U: Iterable<UE>, TU: Thenable<U>> Thenable<T>.thenFlatMapValues(on: Executor? = conf.Q.map, transform: (E) -> TU): Promise<Iterable<UE>> {
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

/**
 * Promise<Iterable<T>>` => `T` -> Bool => `Promise<Iterable<U>>`
 *
 *   firstly {
 *      Promise.value(arrayListOf(1,2,3))
 *   }.filterValues {
 *      it > 1
 *   }.done {
 *      // it => [2,3]
 *   }
 */
inline fun <E, T: Iterable<E>> Thenable<T>.filterValues(on: Executor? = conf.Q.map, crossinline isIncluded: (E) -> Boolean): Promise<Iterable<E>> {
    return map(on = on) { it.filter(isIncluded)}
}

/**
 * Returns a promise fulfilled with the first value of this `Iterable` or, if empty, a promise rejected with PMKError.emptySequence.
 */
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

/**
 * Returns a promise fulfilled with the last value of this `Iterable` or, if empty, a promise rejected with PMKError.emptySequence.
 */
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

/**
 * Returns a promise fulfilled with the sorted values of this `Iterable`.
 */
inline fun <E: Comparable<E>, T: Iterable<E>> Thenable<T>.sortedValues(on: Executor? = conf.Q.map, crossinline selector: (E) -> E?): Promise<Iterable<E>> {
    return map(on = on) { it.sortedBy(selector) }
}
