package com.opinyour.android.app.data.utils

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.example.madam.data.localCaches.UserLocalCache
import com.example.madam.data.db.repositories.AppDatabase
import com.example.madam.data.db.repositories.UserRepository
import com.example.madam.data.db.repositories.VideoRepository
import com.example.madam.data.localCaches.VideoLocalCache
import com.opinyour.android.app.data.api.WebApi

object Injection {

    private fun provideUserCache(context: Context): UserLocalCache {
        val database = AppDatabase.getInstance(context)
        return UserLocalCache(database.appUserDao())
    }

    fun provideUserRepository(context: Context): UserRepository {
        return UserRepository.getInstance(provideUserCache(context))
    }

    private fun provideVideoCache(context: Context): VideoLocalCache {
        val database = AppDatabase.getInstance(context)
        return VideoLocalCache(database.appVideoDao())
    }

    fun provideVideoRepository(context: Context): VideoRepository {
        return VideoRepository.getInstance(WebApi.create(), provideVideoCache(context))
    }

    fun provideViewModelFactory(context: Context): ViewModelProvider.Factory {
        return ViewModelFactory(
            provideUserRepository(
                context
            ),
            provideVideoRepository(
                context
            )
        )
    }
}
