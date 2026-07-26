package com.example.shiyu.data.dao

import androidx.room.*
import com.example.shiyu.data.entity.EbookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EbookDao {
    @Query("SELECT * FROM ebooks ORDER BY lastReadAt DESC")
    fun getAllEbooks(): Flow<List<EbookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEbook(ebook: EbookEntity)

    @Query("DELETE FROM ebooks WHERE id = :id")
    suspend fun deleteEbookById(id: String)
}
