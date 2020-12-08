package com.example.madam.ui.viewModels


import androidx.lifecycle.*
import com.example.madam.data.db.repositories.UserRepository
import com.example.madam.data.db.repositories.VideoRepository
import com.example.madam.data.db.repositories.model.VideoItem
import com.example.madam.utils.UserManager
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import java.io.File

class VideoViewModel(
    private val repository: VideoRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val apiPrefix = "http://api.mcomputing.eu/mobv/uploads/"

    var userManager: UserManager = UserManager(userRepository)

    val error: MutableLiveData<String> = MutableLiveData()
    val success: MutableLiveData<String> = MutableLiveData()

    private var lastCallFailed = false
    var apiCallFunctionFailed: String = ""
    var videoFile: File? = null
    var lastItem: VideoItem? = null

    val videos: LiveData<List<VideoItem>>
        get() = transform(repository.getVideos())

    fun loadVideos() {
        viewModelScope.launch {
            if (userManager.getLoggedUser() == null) {
                return@launch
            }
            repository.loadVideos(userManager.getLoggedUser()!!) {
                if (!lastCallFailed) {
                    lastCallFailed = true
                }
                if (lastCallFailed && it == "Bad request token") {
                    userManager.refreshToken()
                    apiCallFunctionFailed = "loadVideos"
                }
                else {
                    error.postValue(it)
                }
            }
        }
    }

    fun uploadVideo(videoFile: File) {
        this.videoFile = videoFile
        viewModelScope.launch {
            userManager.getLoggedUser()!!.token!!.let { token ->
                repository.addVideo(
                    videoFile,
                    token,
                    {
                        success.postValue(it)
                        lastCallFailed = false
                    },
                    {
                        if (!lastCallFailed) {
                            lastCallFailed = true
                        }
                        if (lastCallFailed && it == "Bad request token") {
                            userManager.refreshToken()
                            apiCallFunctionFailed = "uploadVideo"
                        }
                        else {
                            error.postValue(it)
                        }
                    }
                )
            }
        }
    }

    fun deleteVideo(item: VideoItem) {
        lastItem = item
        viewModelScope.launch {
            repository.deleteVideo(item, userManager.getLoggedUser()!!, {
                if (it == "Wrong token") {
                    userManager.refreshToken()
                    apiCallFunctionFailed = "deleteVideo"
                }},
                {
                  success.postValue(it)
                })
        }
    }

    fun hardLogout() {
        userManager.logoutUser()
    }

    private fun transform(videos: LiveData<List<VideoItem>>): LiveData<List<VideoItem>> {
        return Transformations.map(videos) { items ->
            items.map {
                VideoItem(
                    id = it.id,
                    video_url = if (it.video_url.isNotBlank()) {
                        apiPrefix + it.video_url
                    } else {
                        it.video_url
                    },
                    username = it.username,
                    created_at = it.created_at,
                    user_image_url = if (it.user_image_url.isNotBlank()) {
                        apiPrefix + it.user_image_url
                    } else {
                        it.user_image_url
                    }
                )
            }
        }
    }
}
