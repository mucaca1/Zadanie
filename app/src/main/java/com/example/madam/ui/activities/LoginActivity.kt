package com.example.madam.ui.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.example.madam.R
import com.example.madam.ui.adapters.PagerAdapter
import com.example.madam.ui.fragments.LoginFragment
import com.example.madam.ui.fragments.RegistrationFragment
import com.example.madam.utils.InternetHelper
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    var pagerAdapter: PagerAdapter = PagerAdapter(this)
    var isLogged: MutableLiveData<Boolean> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (!InternetHelper.isNetworkAvailable(this)) {
            Toasty.warning(applicationContext, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        if (view_login_pager != null) {
            pagerAdapter.addFragment(LoginFragment())
            pagerAdapter.addFragment(RegistrationFragment())
            view_login_pager.adapter = pagerAdapter
        }

        isLogged.observe(this) {
            if (it) {
                val myIntent = Intent(this, MainActivity::class.java)
                myIntent.putExtra("login", it)
                this.startActivity(myIntent)
            }
        }
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetwork
        return activeNetworkInfo != null && connectivityManager.isActiveNetworkMetered
    }

}