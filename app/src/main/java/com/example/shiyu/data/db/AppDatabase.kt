package com.example.shiyu.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.shiyu.data.dao.*
import com.example.shiyu.data.entity.*

@Database(
    entities = [
        ArticleEntity::class,
        VocabularyEntity::class,
        SentenceEntity::class,
        SettingEntity::class,
        EbookEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun settingDao(): SettingDao
    abstract fun ebookDao(): EbookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shiyu.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
