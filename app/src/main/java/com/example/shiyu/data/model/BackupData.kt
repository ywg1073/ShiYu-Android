package com.example.shiyu.data.model

import com.example.shiyu.data.entity.ArticleEntity
import com.example.shiyu.data.entity.SentenceEntity
import com.example.shiyu.data.entity.SettingEntity
import com.example.shiyu.data.entity.VocabularyEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val articles: List<ArticleEntity> = emptyList(),
    val vocabulary: List<VocabularyEntity> = emptyList(),
    val sentences: List<SentenceEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList()
)
