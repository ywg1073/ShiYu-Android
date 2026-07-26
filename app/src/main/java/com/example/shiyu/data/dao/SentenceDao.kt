package com.example.shiyu.data.dao

import androidx.room.*
import com.example.shiyu.data.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences ORDER BY createdAt DESC")
    fun getAllSentences(): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getSentenceById(id: String): SentenceEntity?

    @Query("SELECT * FROM sentences WHERE srsDue IS NULL OR srsDue <= :now ORDER BY srsDue ASC")
    suspend fun getDueSentences(now: Long): List<SentenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(item: SentenceEntity)

    @Update
    suspend fun updateSentence(item: SentenceEntity)

    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun deleteSentenceById(id: String)

    @Query("DELETE FROM sentences")
    suspend fun deleteAllSentences()
}
