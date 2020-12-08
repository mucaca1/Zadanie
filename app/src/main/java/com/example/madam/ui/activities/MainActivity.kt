package com.example.madam.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.madam.R
import com.example.madam.ui.adapters.PagerAdapter
import com.example.madam.ui.fragments.HomeFragment
import com.example.madam.ui.fragments.ProfileFragment
import com.example.madam.ui.fragments.VideoRecordFragment
import com.example.madam.utils.InternetHelper
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*


/**
 * MaDaM - My dáme momentku
 * contributors :
 * - Michal Roháček
 * - Matej Krč
 * - Daniel Vaník
 */
class MainActivity : AppCompatActivity() {

    var pagerAdapter: PagerAdapter = PagerAdapter(this)
    var isLogged: MutableLiveData<Boolean> = MutableLiveData()
    var permissionStatus: MutableLiveData<Boolean> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!InternetHelper.isNetworkAvailable(this)) {
            Toasty.warning(applicationContext, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        if (view_main_pager != null) {
            pagerAdapter.addFragment(ProfileFragment())
            pagerAdapter.addFragment(HomeFragment())
            pagerAdapter.addFragment(VideoRecordFragment())
            view_main_pager.adapter = pagerAdapter
            view_main_pager.currentItem = 1
        }

        isLogged.observe(this, androidx.lifecycle.Observer {
            if (!it) {
                goToActivity(LoginActivity::class.java)
            }
        })
        isLogged.value = intent.extras?.getBoolean("login")

        requestPermissions(permissions, REQUEST_PERMISSIONS_OK_CODE)

        val policy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun <T> goToActivity(cls: Class<T>) {
        val myIntent = Intent(this, cls)
        this.startActivity(myIntent)
    }

    override fun onResume() {
        super.onResume()
        view_main_pager.postDelayed({
            view_main_pager.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    companion object {
        const val FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
        const val REQUEST_PERMISSIONS_OK_CODE = 0
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_OK_CODE && permissions.isNotEmpty()) {
            if (grantResults[0] == -1 && grantResults[1] == -1 && grantResults[2] == -1 &&
                grantResults[3] == -1 && grantResults[4] == 0 && grantResults[5] == 0
            ) {
                // ok
                Log.i("Perm", "Success")
                permissionStatus.value = true
            } else {
                // bad
                Log.i("Perm", "Error")
                permissionStatus.value = false
            }
        }
    }

}