package com.example.shiyu.data.repository

import com.example.shiyu.data.dao.VocabularyDao
import com.example.shiyu.data.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

class VocabularyRepository(private val vocabularyDao: VocabularyDao) {
    val allVocabulary: Flow<List<VocabularyEntity>> = vocabularyDao.getAllVocabulary()

    suspend fun getVocabularyById(id: String): VocabularyEntity? = vocabularyDao.getVocabularyById(id)

    suspend fun getVocabularyByWord(word: String): VocabularyEntity? = vocabularyDao.getVocabularyByWord(word)

    suspend fun getDueVocabulary(now: Long = System.currentTimeMillis()): List<VocabularyEntity> =
        vocabularyDao.getDueVocabulary(now)

    suspend fun insertVocabulary(item: VocabularyEntity) = vocabularyDao.insertVocabulary(item)

    suspend fun updateVocabulary(item: VocabularyEntity) = vocabularyDao.updateVocabulary(item)

    suspend fun deleteVocabularyById(id: String) = vocabularyDao.deleteVocabularyById(id)

    suspend fun deleteAllVocabulary() = vocabularyDao.deleteAllVocabulary()
}
