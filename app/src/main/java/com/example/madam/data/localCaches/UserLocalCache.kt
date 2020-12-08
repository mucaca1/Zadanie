package com.example.madam.data.localCaches

import com.example.madam.data.db.daos.DbUserDao
import com.example.madam.data.db.repositories.model.UserItem

class UserLocalCache(private val userDao: DbUserDao) {

    suspend fun getAll(): List<UserItem> {
        return userDao.getAll()
    }

    suspend fun loadAllByIds(userIds: IntArray): List<UserItem> {
        return userDao.loadAllByIds(userIds)
    }

    suspend fun findByLogin(login: String): UserItem {
        return userDao.findByLogin(login)
    }

    suspend fun insertAll(user: UserItem) {
        return userDao.insertAll(user)
    }

    suspend fun delete(user: UserItem) {
        userDao.delete(user)
    }

    suspend fun getFirstUser(): UserItem? {
        return userDao.getFirstUser()
    }

    suspend fun update(userItem: UserItem) {
        userDao.update(userItem)
    }
}