package com.example.madam.utils

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.madam.data.api.model.UserResponse
import com.example.madam.data.db.repositories.UserRepository
import com.example.madam.data.db.repositories.model.UserItem
import com.opinyour.android.app.data.api.WebApi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManager(private val userRepository: UserRepository) {

    var refreshTokenSuccess: MutableLiveData<Boolean> = MutableLiveData()

    fun isLogged() = runBlocking<Boolean> {
        return@runBlocking userRepository.getLoggedUser() != null
    }

    fun logoutUser() = runBlocking {
        userRepository.logoutUser()
    }

    fun loginUser(user: UserItem) = runBlocking {
        userRepository.loginUser(user)
    }

    fun updateUser(user: UserItem) = runBlocking {
        userRepository.update(user)
    }

    fun getLoggedUser() = runBlocking<UserItem?> {
        return@runBlocking userRepository.getLoggedUser()
    }

    fun refreshToken() {
        val jsonObject = JSONObject()
        jsonObject.put("action", "refreshToken")
        jsonObject.put("apikey", WebApi.API_KEY)
        jsonObject.put("refreshToken", getLoggedUser()?.refreshToken)
        val body = jsonObject.toString()
        val data = RequestBody.create(MediaType.parse("application/json"), body)

        var response: Call<UserResponse> = WebApi.create().changePassword(data)
        response.enqueue(object : Callback<UserResponse> {
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("fail", t.message.toString())
                refreshTokenSuccess.value = false
            }

            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                if (response.code() == 200) {
                    updateUser(
                        UserItem(
                            response.body()?.username.toString(),
                            response.body()?.email.toString(),
                            response.body()?.token.toString(),
                            response.body()?.refresh.toString(),
                            response.body()?.profile.toString()
                        )
                    )
                    Log.i("success", response.body()?.id.toString())
                    refreshTokenSuccess.value = true
                } else {
                    Log.i("success", "Bad refresh params")
                    refreshTokenSuccess.value = false
                }
            }
        })
    }
}
