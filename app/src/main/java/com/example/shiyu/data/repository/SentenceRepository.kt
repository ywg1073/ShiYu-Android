package com.example.shiyu.data.repository

import com.example.shiyu.data.dao.SentenceDao
import com.example.shiyu.data.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

class SentenceRepository(private val sentenceDao: SentenceDao) {
    val allSentences: Flow<List<SentenceEntity>> = sentenceDao.getAllSentences()

    suspend fun getSentenceById(id: String): SentenceEntity? = sentenceDao.getSentenceById(id)

    suspend fun getDueSentences(now: Long = System.currentTimeMillis()): List<SentenceEntity> =
        sentenceDao.getDueSentences(now)

    suspend fun insertSentence(item: SentenceEntity) = sentenceDao.insertSentence(item)

    suspend fun updateSentence(item: SentenceEntity) = sentenceDao.updateSentence(item)

    suspend fun deleteSentenceById(id: String) = sentenceDao.deleteSentenceById(id)

    suspend fun deleteAllSentences() = sentenceDao.deleteAllSentences()
}
