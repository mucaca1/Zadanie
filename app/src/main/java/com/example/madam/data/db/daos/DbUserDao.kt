package com.example.madam.data.db.daos

import androidx.room.*
import com.example.madam.data.db.repositories.model.UserItem

@Dao
interface DbUserDao {

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserItem>

    @Query("SELECT * FROM users WHERE username IN (:userIds)")
    suspend fun loadAllByIds(userIds: IntArray): List<UserItem>

    @Query("SELECT * FROM users WHERE username LIKE :login LIMIT 1")
    suspend fun findByLogin(login: String): UserItem

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserItem

    @Update
    suspend fun update(user: UserItem)

    @Insert
    suspend fun insertAll(vararg user: UserItem)

    @Delete
    suspend fun delete(user: UserItem)
}