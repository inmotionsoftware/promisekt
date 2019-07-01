package com.inmotionsoftware.androidexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.inmotionsoftware.promisekt.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        showProgressIndicator()

        ExampleService()
            .getUser()
            .map { it.copy(name = it.name.toUpperCase()) }
            .done { user ->
                this.name.text = user.name
                this.email.text = user.email
                this.website.text = user.website
            }
            .ensure { hideProgressIndicator() }
            .catch { it.printStackTrace() }
    }

    private fun showProgressIndicator() {
        this.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator() {
        this.progressBar.visibility = View.GONE
    }
}
