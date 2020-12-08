package com.example.madam.data.db.repositories.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserItem(
    @PrimaryKey val username: String,
    var email: String?,
    var token: String?,
    var refreshToken: String?,
    var profile: String?
) {
    override fun toString(): String {
        return "User [username: $username, email: $email, token: $token, refresh token: $refreshToken, profile: $profile]"
    }
}