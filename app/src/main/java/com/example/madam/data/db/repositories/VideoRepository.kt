package com.example.madam.data.db.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.madam.data.api.model.StatusResponse
import com.example.madam.data.db.repositories.model.UserItem
import com.example.madam.data.db.repositories.model.VideoItem
import com.example.madam.data.localCaches.VideoLocalCache
import com.opinyour.android.app.data.api.WebApi
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.ConnectException

class VideoRepository private constructor(
    private val api: WebApi,
    private val cache: VideoLocalCache
) {

    companion object {
        const val TAG = "VideoRepository"

        @Volatile
        private var INSTANCE: VideoRepository? = null

        fun getInstance(api: WebApi, cache: VideoLocalCache): VideoRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: VideoRepository(api, cache).also { INSTANCE = it }
            }
    }

    fun getVideos(): LiveData<List<VideoItem>> = cache.getVideos()

    suspend fun addVideo(
        videoFile: File,
        token: String,
        onSuccess: (info: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("apikey", WebApi.API_KEY)
            jsonObject.put("token", token)
            val body = jsonObject.toString()
            val data = RequestBody.create(MediaType.parse("application/json"), body)
            val videoRequest = RequestBody.create(MediaType.parse("video/mp4"), videoFile)
            val video = MultipartBody.Part.createFormData("video", videoFile.name, videoRequest)
            val response = api.addPost(video, data)
            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.status == "success") {
                        onSuccess("Successful upload video")
                        return
                    }
                }
            }
            if (response.code() == 401) {
                onError("Bad request token")
            } else {
                onError("Upload video failed")
            }

        } catch (ex: ConnectException) {
            onError("Check internet connection")
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            onError("Upload video failed")
            ex.printStackTrace()
            return
        }
    }

    fun getVideo(id: String): LiveData<VideoItem> = cache.getVideo(id)

    suspend fun loadVideos(user: UserItem, onError: (error: String) -> Unit) {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("action", "posts")
            jsonObject.put("apikey", WebApi.API_KEY)
            jsonObject.put("token", user.token)
            val body = jsonObject.toString()
            val data = RequestBody.create(MediaType.parse("application/json"), body)
            val response = api.getVideos(data)

            if (response.isSuccessful) {
                response.body()?.let {
                    return cache.addVideos(it.map { item ->
                        VideoItem(
                            id = item.postid,
                            video_url = item.videourl,
                            user_image_url = item.profile,
                            username = item.username,
                            created_at = item.created
                        )
                    })
                }
            }

            onError("Load videos failed. Try again later please.")
        } catch (ex: ConnectException) {
            onError("Off-line. Check internet connection.")
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            onError("Oops...Change failed. Try again later please.")
            ex.printStackTrace()
            return
        }
    }

    fun deleteVideo(video: VideoItem, user: UserItem, onError: (error: String) -> Unit, onSuccess: (info: String) -> Unit) {
        val jsonObject = JSONObject()
        jsonObject.put("action", "deletePost")
        jsonObject.put("apikey", WebApi.API_KEY)
        jsonObject.put("token", user.token)
        jsonObject.put("id", video.id)
        val body = jsonObject.toString()
        val data = RequestBody.create(MediaType.parse("application/json"), body)
        val response: Call<StatusResponse> = api.deletePost(data)

        response.enqueue(object : Callback<StatusResponse> {
            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Log.e("fail", t.message.toString())
            }

            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.code() == 200) {
                    cache.deleteVideo(video)
                    Log.i("success", "Post deleted successfully")
                    onSuccess("Post deleted successfully")
                } else if (response.code() == 401) {
                    // Bad token
                    Log.i("error", "Wrong token")
                    onError("Wrong token")
                } else {
                    Log.i("error", "Error")
                    onError("Error")
                }
            }
        })
    }
}



