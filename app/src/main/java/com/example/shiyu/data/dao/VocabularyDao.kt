package com.example.shiyu.data.dao

import androidx.room.*
import com.example.shiyu.data.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary ORDER BY createdAt DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getVocabularyById(id: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE srsDue IS NULL OR srsDue <= :now ORDER BY srsDue ASC")
    suspend fun getDueVocabulary(now: Long): List<VocabularyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(item: VocabularyEntity)

    @Update
    suspend fun updateVocabulary(item: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabularyById(id: String)

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAllVocabulary()
}
