package com.example.shiyu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ebooks")
data class EbookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val author: String? = null,
    val format: String = "epub",
    val progress: Float = 0.0f,
    val cfiPosition: String? = null,
    val lastReadAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
