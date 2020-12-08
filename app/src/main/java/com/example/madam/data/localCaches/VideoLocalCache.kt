package com.example.madam.data.localCaches

import androidx.lifecycle.LiveData
import com.example.madam.data.db.daos.DbVideoDao
import com.example.madam.data.db.repositories.model.VideoItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VideoLocalCache(private val dao: DbVideoDao) {

    suspend fun addVideos(videoItems: List<VideoItem>) {
        dao.addVideos(videoItems)
    }

    suspend fun addVideo(videoItem: VideoItem) {
        dao.addVideo(videoItem)
    }

    suspend fun updateVideo(videoItem: VideoItem) {
        dao.updateVideo(videoItem)
    }

    fun deleteVideo(videoItem: VideoItem) {
        GlobalScope.launch { dao.deleteVideo(videoItem) }
    }

    fun getVideos(): LiveData<List<VideoItem>> = dao.loadVideos()

    fun getVideo(id: String): LiveData<VideoItem> = dao.loadVideo(id)

}