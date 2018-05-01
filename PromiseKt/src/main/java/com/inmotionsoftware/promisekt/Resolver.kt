package com.inmotionsoftware.promisekt

class Resolver<T>(val box: Box<Result<T>>) {
    // This is probably not working as intended in the JVM
    protected fun finallize() {
        when (this.box.inspect()) {
            is Sealant.pending -> print("PMKPromise: warning: pending promise deallocated")
        }
    }
}

fun <T> Resolver<T>.fulfill(value: T) {
    this.box.seal(Result.fulfilled(value))
}

fun <T> Resolver<T>.reject(error: Throwable) {
    this.box.seal(Result.rejected(error))
}

fun <T> Resolver<T>.resolve(result: Result<T>) {
    this.box.seal(result)
}

fun <T> Resolver<T>.resolve(value: T?, error: Throwable?) {
    when {
        error != null -> this.reject(error)
        value != null -> this.fulfill(value)
        else -> this.reject(PMKError.invalidCallingConvention())
    }
}

fun Resolver<Unit>.resolve(error: Throwable?) {
    when {
        error != null -> this.reject(error)
        else -> this.fulfill(value = Unit)
    }
}

sealed class Result<T>{
    class fulfilled<T>(val value: T): Result<T>()
    class rejected<T>(val error: Throwable): Result<T>()
}

val <T> Result<T>.isFulfilled: Boolean get() {
    return when (this) {
        is Result.fulfilled -> true
        else -> false
    }
}
