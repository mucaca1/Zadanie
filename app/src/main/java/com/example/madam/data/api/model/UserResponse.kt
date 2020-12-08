package com.example.madam.data.api.model

data class UserResponse(
    var id: String,
    var username: String,
    var email: String,
    var token: String,
    var refresh: String,
    var profile: String
) {
    override fun toString(): String {
        return "User [username: $username, email: $email, token: $token, refresh token: $refresh, profile: $profile]"
    }
}