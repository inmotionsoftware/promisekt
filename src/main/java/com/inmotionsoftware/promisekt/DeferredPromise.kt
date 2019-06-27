package com.inmotionsoftware.promisekt

class DeferredPromise<T> {
    val promise: Promise<T>
    private lateinit var resolver: Resolver<T>

    init {
        this.promise = Promise { this.resolver = it }
    }

    fun resolve(value: T) {
        this.resolver.fulfill(value)
    }

    fun reject(error: Throwable) {
        this.resolver.reject(error)
    }
}
