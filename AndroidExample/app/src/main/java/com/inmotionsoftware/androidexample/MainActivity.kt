package com.inmotionsoftware.androidexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.inmotionsoftware.promisekt.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadData()

        this.reloadButton.setOnClickListener {
            loadData()
        }
    }

    private fun loadData() {
        this.name.text = ""
        this.email.text = ""
        this.website.text = ""

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
            .catch { showErrorDialog(it) }
    }

    private fun showProgressIndicator() {
        this.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator() {
        this.progressBar.visibility = View.GONE
    }

    private fun showErrorDialog(t: Throwable) {
        ErrorDialog()
            .apply { arguments = Bundle().apply { putString("message", t.message) } }
            .show(supportFragmentManager, "errorDialog")
    }
}

class ErrorDialog : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return TextView(context).apply {
            setPadding(30)
            text = arguments?.getString("message")
        }
    }
}
