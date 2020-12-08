package com.example.madam.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager

class InternetHelper {

    companion object {
        fun isNetworkAvailable(activity: Activity): Boolean {
            val connectivityManager =
                activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetwork
            return activeNetworkInfo != null && connectivityManager.isDefaultNetworkActive
        }
    }

}