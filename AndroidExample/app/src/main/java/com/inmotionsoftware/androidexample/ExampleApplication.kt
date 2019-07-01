package com.inmotionsoftware.androidexample

import android.app.Application
import com.inmotionsoftware.promisekt.PMKConfiguration

class ExampleApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        PMKConfiguration.Q = PMKConfiguration.Value(DispatchExecutor.main, DispatchExecutor.main)
    }
}