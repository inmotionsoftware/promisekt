package com.inmotionsoftware.promisekt

sealed class PMKError: Throwable {
    constructor(): super()
    constructor(message: String): super(message = message)
    constructor(cause: Throwable): super(cause = cause)
    constructor(message: String, cause: Throwable): super(message = message, cause = cause)

    class invalidCallingConvention:
            PMKError("A closure was called with an invalid calling convention, probably (null, null)")
    class returnedSelf:
            PMKError("A promise handler returned itself")
    class badInput:
            PMKError("Bad input was provided to a IMSPromise function")
    class cancelled:
            PMKError("The asynchronous sequence was cancelled")
    class compactMap /*(val obj: Any, val type: Class<Any>) */:
//            PMKError("Could not compactMap<($type)>: ($obj)")
            PMKError("Could not compactMap")
    class emptySequence:
            PMKError("The first or last element was requested for an empty sequence")
}

interface CancellableError {
    val isCancelled: Boolean
}

val Throwable.isCancelled: Boolean get() {
    try {
        throw this
    } catch (e: PMKError.cancelled) {
        return true
    } catch (e: Throwable) {
        return if (e is CancellableError) { e.isCancelled } else { false }
    }
}

enum class CatchPolicy {
    allErrors
    , allErrorsExceptCancellation
}
