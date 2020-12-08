package com.example.madam.data.db.repositories.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "created_at")
    val created_at: String,

    @ColumnInfo(name = "video_url")
    val video_url: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "user_image_url")
    val user_image_url: String,
)