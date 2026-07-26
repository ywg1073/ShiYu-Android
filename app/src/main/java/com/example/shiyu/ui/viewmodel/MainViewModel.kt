package com.example.shiyu.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shiyu.data.InitialData
import com.example.shiyu.data.db.AppDatabase
import com.example.shiyu.data.entity.ArticleEntity
import com.example.shiyu.data.entity.SentenceEntity
import com.example.shiyu.data.entity.SettingEntity
import com.example.shiyu.data.entity.VocabularyEntity
import com.example.shiyu.data.repository.ArticleRepository
import com.example.shiyu.data.repository.SentenceRepository
import com.example.shiyu.data.repository.SettingRepository
import com.example.shiyu.data.repository.VocabularyRepository
import com.example.shiyu.fsrs.FsrsCard
import com.example.shiyu.fsrs.FsrsRating
import com.example.shiyu.fsrs.FsrsScheduler
import com.example.shiyu.fsrs.FsrsState
import com.example.shiyu.service.DeepSeekService
import com.example.shiyu.service.TtsService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class ReviewItemType {
    VOCABULARY,
    SENTENCE
}

data class ReviewItem(
    val type: ReviewItemType,
    val id: String,
    val front: String,
    val back: String,
    val context: String? = null,
    val articlePath: String? = null,
    val fsrsCard: FsrsCard,
    val rawVocab: VocabularyEntity? = null,
    val rawSentence: SentenceEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val articleRepo = ArticleRepository(db.articleDao())
    private val vocabularyRepo = VocabularyRepository(db.vocabularyDao())
    private val sentenceRepo = SentenceRepository(db.sentenceDao())
    private val settingRepo = SettingRepository(db.settingDao())

    val ttsService = TtsService(application)
    val deepSeekService = DeepSeekService()

    val articles: StateFlow<List<ArticleEntity>> = articleRepo.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vocabulary: StateFlow<List<VocabularyEntity>> = vocabularyRepo.allVocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentences: StateFlow<List<SentenceEntity>> = sentenceRepo.allSentences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<List<SettingEntity>> = settingRepo.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentArticle = MutableStateFlow<ArticleEntity?>(null)
    val currentArticle: StateFlow<ArticleEntity?> = _currentArticle.asStateFlow()

    private val _readerFontSize = MutableStateFlow("medium")
    val readerFontSize: StateFlow<String> = _readerFontSize.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _reviewQueue = MutableStateFlow<List<ReviewItem>>(emptyList())
    val reviewQueue: StateFlow<List<ReviewItem>> = _reviewQueue.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if DB is empty and initialize sample data
            articles.first().let { currentList ->
                if (currentList.isEmpty()) {
                    initSampleData()
                }
            }
            loadReviewQueue()
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setCurrentArticle(article: ArticleEntity?) {
        _currentArticle.value = article
    }

    fun setReaderFontSize(size: String) {
        _readerFontSize.value = size
    }

    private suspend fun initSampleData() {
        InitialData.sampleArticles.forEach { articleRepo.insertArticle(it) }
        InitialData.sampleVocabulary.forEach { vocabularyRepo.insertVocabulary(it) }
        InitialData.sampleSentences.forEach { sentenceRepo.insertSentence(it) }
        if (settingRepo.getSettingValue("deepseek_api_key") == null) {
            settingRepo.saveSetting("deepseek_api_key", "")
        }
        if (settingRepo.getSettingValue("deepseek_model") == null) {
            settingRepo.saveSetting("deepseek_model", "deepseek-v4-flash")
        }
        if (settingRepo.getSettingValue("app_theme") == null) {
            settingRepo.saveSetting("app_theme", "light")
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            initSampleData()
            loadReviewQueue()
            showToast("已重置并加载示例数据！")
        }
    }

    fun loadReviewQueue() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueVocabs = vocabularyRepo.getDueVocabulary(now)
            val dueSentences = sentenceRepo.getDueSentences(now)

            val queue = mutableListOf<ReviewItem>()

            dueVocabs.forEach { v ->
                val card = FsrsCard(
                    due = v.srsDue ?: now,
                    stability = v.srsStability,
                    difficulty = v.srsDifficulty,
                    state = FsrsState.entries.getOrElse(v.srsState) { FsrsState.NEW },
                    lapses = v.srsLapses,
                    reps = v.srsReps,
                    lastReview = v.srsLastReview
                )
                queue.add(
                    ReviewItem(
                        type = ReviewItemType.VOCABULARY,
                        id = v.id,
                        front = v.word,
                        back = v.meaning,
                        context = v.context,
                        articlePath = v.articlePath,
                        fsrsCard = card,
                        rawVocab = v
                    )
                )
            }

            dueSentences.forEach { s ->
                val card = FsrsCard(
                    due = s.srsDue ?: now,
                    stability = s.srsStability,
                    difficulty = s.srsDifficulty,
                    state = FsrsState.entries.getOrElse(s.srsState) { FsrsState.NEW },
                    lapses = s.srsLapses,
                    reps = s.srsReps,
                    lastReview = s.srsLastReview
                )
                queue.add(
                    ReviewItem(
                        type = ReviewItemType.SENTENCE,
                        id = s.id,
                        front = s.sentence,
                        back = s.explanation,
                        articlePath = s.articlePath,
                        fsrsCard = card,
                        rawSentence = s
                    )
                )
            }

            queue.sortBy { it.fsrsCard.due }
            _reviewQueue.value = queue
        }
    }

    fun gradeReviewItem(item: ReviewItem, rating: FsrsRating) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = FsrsScheduler.review(item.fsrsCard, rating, now)
            val updatedCard = result.card

            if (item.type == ReviewItemType.VOCABULARY && item.rawVocab != null) {
                val updated = item.rawVocab.copy(
                    reviewCount = item.rawVocab.reviewCount + 1,
                    lastReviewedAt = now,
                    srsDue = updatedCard.due,
                    srsStability = updatedCard.stability,
                    srsDifficulty = updatedCard.difficulty,
                    srsState = updatedCard.state.value,
                    srsLapses = updatedCard.lapses,
                    srsReps = updatedCard.reps,
                    srsLastReview = updatedCard.lastReview
                )
                vocabularyRepo.updateVocabulary(updated)
            } else if (item.type == ReviewItemType.SENTENCE && item.rawSentence != null) {
                val updated = item.rawSentence.copy(
                    reviewCount = item.rawSentence.reviewCount + 1,
                    lastReviewedAt = now,
                    srsDue = updatedCard.due,
                    srsStability = updatedCard.stability,
                    srsDifficulty = updatedCard.difficulty,
                    srsState = updatedCard.state.value,
                    srsLapses = updatedCard.lapses,
                    srsReps = updatedCard.reps,
                    srsLastReview = updatedCard.lastReview
                )
                sentenceRepo.updateSentence(updated)
            }

            _reviewQueue.value = _reviewQueue.value.filter { it.id != item.id }
            showToast("已提交评级: ${rating.label}")
        }
    }

    fun addArticle(title: String, content: String, author: String? = null, category: String? = null, description: String? = null) {
        viewModelScope.launch {
            val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }.toLong()
            val article = ArticleEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Untitled Article" },
                content = content,
                author = author,
                category = category ?: "General",
                description = description,
                wordCount = wordCount
            )
            articleRepo.insertArticle(article)
            showToast("文章保存成功！")
        }
    }

    // Local In-Memory Cache for DeepSeek word query results
    private val wordMeaningCache = mutableMapOf<String, String>()

    suspend fun lookupWord(word: String): String {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return "请输入有效单词"

        // 1. Priority: Local Database / Dictionary Bank
        val localVocab = vocabularyRepo.getVocabularyByWord(clean)
        if (localVocab != null && localVocab.meaning.isNotBlank()) {
            return "📚 【本地智库已归档释义】\n• 单词: ${localVocab.word}\n• 释义: ${localVocab.meaning}" +
                    (if (!localVocab.context.isNullOrBlank()) "\n• 上下文: ${localVocab.context}" else "")
        }

        // 2. Priority: In-Memory Cache (from previous DeepSeek query)
        if (wordMeaningCache.containsKey(clean)) {
            return wordMeaningCache[clean] ?: ""
        }

        // 3. Call DeepSeek API
        val apiKey = getSetting("deepseek_api_key")
        val model = getSetting("deepseek_model") ?: "deepseek-v4-flash"
        val result = deepSeekService.translateOrAnalyze(word, "word", apiKey, model)

        // 4. Cache the result locally so DeepSeek is not called repeatedly
        if (result.isNotBlank()) {
            wordMeaningCache[clean] = result
        }

        return result
    }

    suspend fun generateArticleMindMap(article: ArticleEntity): String {
        val apiKey = getSetting("deepseek_api_key")
        val model = getSetting("deepseek_model") ?: "deepseek-v4-flash"
        val textToAnalyze = "标题: ${article.title}\n\n内容:\n${article.content}"
        val mindmapResult = deepSeekService.translateOrAnalyze(textToAnalyze, "mindmap", apiKey, model)

        if (mindmapResult.isNotBlank()) {
            val updated = article.copy(mindmapMarkdown = mindmapResult)
            articleRepo.updateArticle(updated)
            if (_currentArticle.value?.id == article.id) {
                _currentArticle.value = updated
            }
            showToast("DeepSeek 思维导图生成完成！")
        }
        return mindmapResult
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch {
            articleRepo.deleteArticleById(id)
            if (_currentArticle.value?.id == id) {
                _currentArticle.value = null
            }
            showToast("已删除文章")
        }
    }

    fun addVocabulary(word: String, meaning: String, context: String? = null, articlePath: String? = null) {
        viewModelScope.launch {
            val vocab = VocabularyEntity(
                id = UUID.randomUUID().toString(),
                word = word.trim(),
                meaning = meaning.trim(),
                context = context,
                articlePath = articlePath,
                srsDue = System.currentTimeMillis() // Due immediately for learning
            )
            vocabularyRepo.insertVocabulary(vocab)
            loadReviewQueue()
            showToast("已存入生词本: $word")
        }
    }

    fun deleteVocabulary(id: String) {
        viewModelScope.launch {
            vocabularyRepo.deleteVocabularyById(id)
            loadReviewQueue()
            showToast("已删除生词")
        }
    }

    fun addSentence(sentence: String, explanation: String, articlePath: String? = null) {
        viewModelScope.launch {
            val sen = SentenceEntity(
                id = UUID.randomUUID().toString(),
                sentence = sentence.trim(),
                explanation = explanation.trim(),
                articlePath = articlePath,
                srsDue = System.currentTimeMillis()
            )
            sentenceRepo.insertSentence(sen)
            loadReviewQueue()
            showToast("已存入句法本")
        }
    }

    fun analyzeAndAddSentence(sentence: String, articlePath: String? = null) {
        val cleanSentence = sentence.trim()
        if (cleanSentence.isBlank()) return

        viewModelScope.launch {
            val tempId = UUID.randomUUID().toString()
            val tempSen = SentenceEntity(
                id = tempId,
                sentence = cleanSentence,
                explanation = "⏳ DeepSeek 正在透彻剖析句法结构、从句关系与词汇注解...",
                articlePath = articlePath,
                srsDue = System.currentTimeMillis()
            )
            sentenceRepo.insertSentence(tempSen)
            loadReviewQueue()
            showToast("已保存至句法本，正在生成 AI 句法剖析...")

            val apiKey = getSetting("deepseek_api_key")
            val model = getSetting("deepseek_model") ?: "deepseek-v4-flash"
            val analysis = deepSeekService.translateOrAnalyze(cleanSentence, "complex_sentence", apiKey, model)

            val updated = tempSen.copy(explanation = analysis)
            sentenceRepo.updateSentence(updated)
            loadReviewQueue()
            showToast("✨ DeepSeek 句法剖析生成成功！")
        }
    }

    fun deleteSentence(id: String) {
        viewModelScope.launch {
            sentenceRepo.deleteSentenceById(id)
            loadReviewQueue()
            showToast("已删除句子")
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            settingRepo.saveSetting(key, value)
            showToast("设置已保存")
        }
    }

    suspend fun testDeepSeekConnection(apiKey: String, modelName: String): Result<String> {
        return deepSeekService.testConnection(apiKey, modelName)
    }

    suspend fun exportDataJson(): String {
        val jsonFormat = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val settingsList = settingRepo.allSettings.first()
        val backup = com.example.shiyu.data.model.BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            articles = articles.value,
            vocabulary = vocabulary.value,
            sentences = sentences.value,
            settings = settingsList
        )
        return jsonFormat.encodeToString(backup)
    }

    suspend fun importDataJson(jsonString: String): Result<String> {
        val jsonFormat = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return try {
            val backup = jsonFormat.decodeFromString<com.example.shiyu.data.model.BackupData>(jsonString)
            
            backup.articles.forEach { articleRepo.insertArticle(it) }
            backup.vocabulary.forEach { vocabularyRepo.insertVocabulary(it) }
            backup.sentences.forEach { sentenceRepo.insertSentence(it) }
            backup.settings.forEach { settingRepo.saveSetting(it.key, it.value) }

            loadReviewQueue()
            
            val summary = "成功导入 ${backup.articles.size} 篇文章、${backup.vocabulary.size} 个生词、${backup.sentences.size} 句难句！"
            showToast(summary)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSetting(key: String): String? {
        return settingRepo.getSettingValue(key)
    }

    fun speak(text: String) {
        ttsService.speak(text) { msg ->
            showToast(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}
