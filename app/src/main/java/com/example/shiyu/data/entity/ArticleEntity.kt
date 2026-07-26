package com.example.shiyu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val author: String? = null,
    val category: String? = null,
    val description: String? = null,
    val wordCount: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val mindmapMarkdown: String? = null
)
