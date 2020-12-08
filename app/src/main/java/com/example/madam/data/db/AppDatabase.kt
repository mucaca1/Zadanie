package com.example.madam.data.db.repositories

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.madam.data.db.daos.DbUserDao
import com.example.madam.data.db.daos.DbVideoDao
import com.example.madam.data.db.repositories.model.UserItem
import com.example.madam.data.db.repositories.model.VideoItem

@Database(
    entities = [UserItem::class, VideoItem::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appUserDao(): DbUserDao
    abstract fun appVideoDao(): DbVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "Madam.db"
            ).fallbackToDestructiveMigration()
                .build()
    }
}