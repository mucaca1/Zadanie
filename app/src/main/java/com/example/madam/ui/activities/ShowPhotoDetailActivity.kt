package com.example.madam.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.madam.R
import com.example.madam.ui.fragments.ShowPhotoDetailFragment
import com.example.madam.utils.InternetHelper
import es.dmoral.toasty.Toasty

class ShowPhotoDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_photo_detail)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.show_photo_detail_activity, ShowPhotoDetailFragment.newInstance())
                .commitNow()
        }

        if (!InternetHelper.isNetworkAvailable(this)) {
            Toasty.warning(applicationContext, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }
}