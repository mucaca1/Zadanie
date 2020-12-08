package com.example.madam.data.db.repositories

import com.example.madam.data.db.repositories.model.UserItem
import com.example.madam.data.localCaches.UserLocalCache

class UserRepository private constructor(
    private val cache: UserLocalCache
) {

    companion object {
        const val TAG = "UserRepository"

        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(cache: UserLocalCache): UserRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: UserRepository(cache).also { INSTANCE = it }
            }
    }

    suspend fun getUsers(): List<UserItem> = cache.getAll()

    suspend fun loginUser(userItem: UserItem) {
        cache.insertAll(userItem)
    }

    suspend fun getLoggedUser(): UserItem? {
        return cache.getFirstUser()
    }

    suspend fun logoutUser(userItem: UserItem) {
        cache.delete(userItem)
    }

    suspend fun logoutUser() {
        cache.delete(cache.getFirstUser()!!)
    }

    suspend fun update(userItem: UserItem) {
        cache.update(userItem)
    }
}
