package com.inmotionsoftware.androidexample

import com.inmotionsoftware.promisekt.DeferredPromise
import com.inmotionsoftware.promisekt.Promise
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.*
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class User(
    val name: String,
    val email: String,
    val website: String
)

class ExampleService {

    interface UserService {
        @GET("/users/1")
        fun getUser(): Call<User>
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val service = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(UserService::class.java)


    fun getUser(): Promise<User> {
        val deferred = DeferredPromise<User>()

        service.getUser().enqueue(object :Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    deferred.resolve(response.body()!!)
                } else {
                    deferred.reject(HttpException(response))
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                deferred.reject(t)
            }
        })

        return deferred.promise
    }
}