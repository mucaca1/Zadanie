package com.example.madam.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.madam.R
import com.example.madam.ui.fragments.ChangePasswordFragment
import com.example.madam.utils.InternetHelper
import es.dmoral.toasty.Toasty

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.password_activity, ChangePasswordFragment.newInstance())
                .commitNow()
        }

        if (!InternetHelper.isNetworkAvailable(this)) {
            Toasty.warning(applicationContext, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    fun <T> goToActivity(cls: Class<T>) {
        val myIntent = Intent(this, cls)
        this.startActivity(myIntent)
    }
}