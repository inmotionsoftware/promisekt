package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

sealed class Sealant<R> {
    class pending<R>(val handlers: Handlers<R>): Sealant<R>()
    class resolved<R>(val value: R): Sealant<R>()
}

class Handlers<R> {
    var bodies: ArrayList<(R) -> Unit> = ArrayList()
    fun append(item: (R) -> Unit) { bodies.add(item) }
}

interface Box<T> {
    fun inspect(): Sealant<T>
    fun inspect(sealant: (Sealant<T>) -> Unit)
    fun seal(value: T)
}

class SealedBox<T>(val value: T): Box<T> {
    override fun inspect(): Sealant<T> {
        return Sealant.resolved(value = this.value)
    }

    override fun inspect(sealant: (Sealant<T>) -> Unit) { throw IllegalStateException() }
    override fun seal(value: T) { throw IllegalStateException() }
}

class EmptyBox<T>: Box<T> {
    private var sealant: Sealant<T> = Sealant.pending(Handlers<T>())

    override fun seal(value: T) {
        var handlers: Handlers<T>? = null
        synchronized(lock = this) {
            this.sealant.let {
                when (it) {
                    is Sealant.resolved<*> -> {
                        // Already fulfilled
                        return
                    }
                    is Sealant.pending<T> -> {
                        handlers = it.handlers
                        this.sealant = Sealant.resolved<T>(value = value)
                    }
                }
            }
        }

        //FIXME we are resolved so should `pipe(to:)` be called at this instant, “thens are called in order” would be invalid
        //NOTE we don’t do this in the above `sync` because that could potentially deadlock
        //THOUGH since `then` etc. typically invoke after a run-loop cycle, this issue is somewhat less severe
        handlers?.let {
            it.bodies.forEach { it(value) }
        }

        //TODO solution is an unfortunate third state “sealed” where then's get added
        // to a separate handler pool for that state
        // any other solution has potential races
    }

    override fun inspect(): Sealant<T> {
        return synchronized(lock = this) { this.sealant  }
    }

    override fun inspect(sealant: (Sealant<T>) -> Unit) {
        val body = sealant
        var sealed = false
        synchronized(lock = this) {
            this.sealant.let {
                when (it) {
                    is Sealant.pending -> {
                        // body will append to handlers, so we must stay synchronized
                        body(it)
                    }
                    is Sealant.resolved -> {
                        sealed = true
                    }
                }
            }
        }
        if (sealed) {
            // we do this outside the synchronized to prevent potential deadlocks
            // it's safe because we never transition away from this state
            body(this.sealant)
        }
    }
}
