package com.example.madam.data.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.madam.data.db.repositories.model.VideoItem

@Dao
interface DbVideoDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideos(videoItems: List<VideoItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideo(videoItem: VideoItem)

    @Update
    suspend fun updateVideo(videoItem: VideoItem)

    @Delete
    suspend fun deleteVideo(videoItem: VideoItem)

    @Query("SELECT * FROM videos")
    fun loadVideos(): LiveData<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun loadVideo(id: String): LiveData<VideoItem>


}