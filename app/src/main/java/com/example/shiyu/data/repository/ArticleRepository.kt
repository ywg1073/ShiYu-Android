package com.example.shiyu.data.repository

import com.example.shiyu.data.dao.ArticleDao
import com.example.shiyu.data.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

class ArticleRepository(private val articleDao: ArticleDao) {
    val allArticles: Flow<List<ArticleEntity>> = articleDao.getAllArticles()

    suspend fun getArticleById(id: String): ArticleEntity? = articleDao.getArticleById(id)

    suspend fun insertArticle(article: ArticleEntity) = articleDao.insertArticle(article)

    suspend fun updateArticle(article: ArticleEntity) = articleDao.updateArticle(article)

    suspend fun deleteArticleById(id: String) = articleDao.deleteArticleById(id)

    suspend fun deleteAllArticles() = articleDao.deleteAllArticles()
}
