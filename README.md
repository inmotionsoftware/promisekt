
# Kotlin port of Swift PromiseKit
#### https://github.com/mxcl/PromiseKit

## Setup
PromiseKt allows you to choose which executor you want your code to run on.
For convenience define your main and background Executors

```kotlin
object DispatchExecutor {
    val main: Executor by lazy { Executors.newSingleThreadExecutor() }
    val background: Executor by lazy { Executors.newCachedThreadPool() }
}
```


For Android projects set the main executor to execute on Android's main thread
```kotlin
object DispatchExecutor {
    val main: Executor by lazy { Executor { command -> Handler(Looper.getMainLooper()).post(command) } }
    val background: Executor by lazy { Executors.newCachedThreadPool() }
}
```

Then set PromiseKt to use the main thread as the default thread

```kotlin
PMKConfiguration.Q = PMKConfiguration.Value(DispatchExecutor.main, DispatchExecutor.main)
```

To switch executors a promise executes on you can pass an Executor to any of the operators. 
The operator will use that executor, but will switch back to the main, or default, executor after completion

```kotlin
Promise.value(Unit)
    .map(on = DispatchExecutor.background) { /* execute on background thread */ }
    .map { /* execute on main thread */ }
```

## Examples
```kotlin
showLoadingIndicator()
        
Promise.value(5)
    .map { it * 2 }                                  // transforms value
    .thenMap { Promise.value("the value is: $it") }  // transforms value, but must return another Promise or Guarantee
    .done { setText(text = it) }                     // execute block when promise is complete
    .ensure { hideLoadingIndicator() }               // will be called even if promise fails
    .catch { showErrorDialog(it) }                   // handle exceptions
```

## Wrapping asynchronous api with DeferredPromise (Using Retrofit for example)
```kotlin
fun getNetworkResponse(): Promise<NetworkResponse> {
    val deferred = DeferredPromise<NetworkResponse>()
    this.networkService.enqueue(object: Callback<NetworkResponse> {
        override fun onResponse(call: Call<NetworkResponse>, response: Response<NetworkResponse>) {
            if (response.isSuccessful) {
                deferred.resolve(response.body()!!)
            } else {
                deferred.reject(HttpException(response))
            }
        }
        override fun onFailure(call: Call<NetworkResponse>, t: Throwable) {
            deferred.reject(t)
        }
    })
    
    return deferred.promise
}
```
