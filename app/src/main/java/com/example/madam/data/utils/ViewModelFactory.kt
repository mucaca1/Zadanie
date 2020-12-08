package com.opinyour.android.app.data.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.madam.data.db.repositories.UserRepository
import com.example.madam.data.db.repositories.VideoRepository
import com.example.madam.ui.viewModels.*

/**
 * Factory for ViewModels
 */
class ViewModelFactory(private val userRepository: UserRepository,
                       private val videoRepository: VideoRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository) as T
        }

        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(userRepository) as T
        }

        if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChangePasswordViewModel(userRepository) as T
        }

        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(videoRepository, userRepository) as T
        }

        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
