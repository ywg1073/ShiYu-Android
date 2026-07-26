package com.example.shiyu.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String
)

@Serializable
data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.3
)

@Serializable
data class DeepSeekChoiceMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class DeepSeekChoice(
    val index: Int? = null,
    val message: DeepSeekChoiceMessage? = null
)

@Serializable
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice>? = null
)

class DeepSeekService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun resolveEffectiveModel(modelName: String?): String {
        val raw = modelName?.trim()
        if (!raw.isNullOrBlank()) {
            return raw
        }
        return "deepseek-v4-flash"
    }

    suspend fun translateOrAnalyze(
        text: String,
        promptType: String = "word",
        apiKey: String? = null,
        modelName: String? = null
    ): String = withContext(Dispatchers.IO) {
        val effectiveApiKey = apiKey?.trim()?.takeIf { it.isNotBlank() }
        val effectiveModel = resolveEffectiveModel(modelName)

        // If checking a word, default to online dictionary first
        if (promptType == "word") {
            val onlineDictResult = queryFreeOnlineDictionary(text)
            if (!onlineDictResult.contains("暂无词条释义")) {
                return@withContext onlineDictResult
            }
        }

        if (effectiveApiKey.isNullOrBlank()) {
            return@withContext when (promptType) {
                "word" -> queryFreeOnlineDictionary(text)
                "sentence", "complex_sentence" -> "⚠️ 未配置 DeepSeek API Key，无法生成 AI 句法剖析。请前往【设置】配置 DeepSeek API Key 后重新分析。"
                "mindmap" -> "⚠️ 未配置 DeepSeek API Key，请先前往【设置】填入 API Key 后即可一键生成 AI 思维导图！"
                else -> "⚠️ 未配置 DeepSeek API Key，请前往【设置】填入 API Key。"
            }
        }

        try {
            val systemPrompt = "你是一位精通英语教学、词汇分析与长难句剖析的资深语言专家。请始终使用清晰、美观的 Markdown 结构与 Emoji 符号输出格式化的解析内容。"
            val userPrompt = when (promptType) {
                "word" -> """
                    请详细解析英文单词/词组 '$text'，严格按以下格式输出：
                    📖 【核心释义与音标】
                    • 音标: [美/英音标]
                    • 词性及中文释义
                    
                    💡 【常用搭配与固定短语】
                    • 搭配 1
                    • 搭配 2
                    
                    📝 【经典例句】
                    1. 英文例句 （中文翻译）
                    2. 英文例句 （中文翻译）
                """.trimIndent()

                "sentence" -> """
                    请翻译并解析以下英文句子：
                    "$text"
                    
                    严格按以下格式输出：
                    🎯 【准确译文】
                    中文翻译
                    
                    🔍 【关键结构拆解】
                    • 句式结构说明
                    • 核心动词与修饰成分
                """.trimIndent()

                "complex_sentence" -> """
                    请透彻分析以下长难句：
                    "$text"
                    
                    严格按以下格式输出：
                    🎯 【精准中文译文】
                    中文翻译
                    
                    🧩 【语法主干与结构分析】
                    • 核心主干（主谓宾/主系表）
                    • 从句与修饰语（定语/状语/分词短语）
                    
                    🔑 【重点词汇与短语注解】
                    • 核心词汇及其在本句中的释义
                """.trimIndent()

                "mindmap" -> """
                    请对以下英文文章进行深度结构化剖析，生成包含【中文精细解释与逻辑批注】的高质量思维导图大纲：

                    文章内容：
                    "$text"

                    输出规范（请严格按照 Markdown 结构，确保每一层论点都有中文解释与 💡 批注，不要有任何多余的开场白）：

                    # 📌 [文章英文原名/核心主题] (中文释义与全局总览)
                    
                    ## 1️⃣ 背景与核心概念 (Background & Core Concepts)
                    - 核心概念英文词/句 (中文解释: 详细说明该概念在文中的定位)
                      - 💡 批注: 对背景或核心立意的背景知识、深层内涵进行补充说明
                    
                    ## 2️⃣ 逻辑主干与论点拆解 (Key Arguments & Structure)
                    - 核心论点 1 (英文原文句/精炼观点): 详细中文逻辑解析与论证过程
                      - 💡 批注: 作者的推导逻辑、因果关系或修辞意图批注
                    - 核心论点 2: 观点推演与支撑例证
                      - 💡 批注: 此段落与上下文的承接关系或难点解析
                    
                    ## 3️⃣ 重点语言精髓与批注 (Language Highlights & Notes)
                    - 高频难词/关键短语: 中文释义 + 语境用法
                      - 💡 批注: 经典考点、搭配拓展或近义词辨析
                """.trimIndent()

                else -> "请精准翻译并解析以下英文文本：\n\n$text"
            }

            val reqObj = DeepSeekRequest(
                model = effectiveModel,
                messages = listOf(
                    DeepSeekMessage(role = "system", content = systemPrompt),
                    DeepSeekMessage(role = "user", content = userPrompt)
                )
            )

            val reqJson = json.encodeToString(DeepSeekRequest.serializer(), reqObj)
            val requestBody = reqJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val url = "https://api.deepseek.com/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $effectiveApiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val resObj = json.decodeFromString(DeepSeekResponse.serializer(), bodyStr)
                    val textResult = resObj.choices?.firstOrNull()?.message?.content
                    if (!textResult.isNullOrBlank()) {
                        return@withContext textResult.trim()
                    }
                }
                
                val errorBody = response.body?.string() ?: ""
                val code = response.code
                val userMsg = when {
                    code == 402 || errorBody.contains("balance", ignoreCase = true) || errorBody.contains("insufficient", ignoreCase = true) ->
                        "DeepSeek 账号余额不足或已欠费 (HTTP $code)"
                    code == 401 || (errorBody.contains("invalid", ignoreCase = true) && errorBody.contains("key", ignoreCase = true)) ->
                        "DeepSeek API Key 无效或未授权 (HTTP 401)"
                    code == 400 ->
                        "DeepSeek API 请求失败 (HTTP 400)，详情: ${errorBody.take(80)}"
                    else ->
                        "DeepSeek 响应异常 (HTTP $code)"
                }

                // Auto Fallback to alternative free dictionary or offline mode
                return@withContext when (promptType) {
                    "word" -> {
                        val dictRes = queryFreeOnlineDictionary(text)
                        "$dictRes\n\n💡 提示：已自动切至权威词典查询（$userMsg）"
                    }
                    "sentence", "complex_sentence" -> """
                        🎯 【句子基线分析 (备用模式)】
                        "$text"
                        
                        🧩 【结构提示】
                        • 核心句子已存入句法本，可随时查阅。
                        • 💡 提示：AI 深度剖析暂不可用（$userMsg）
                    """.trimIndent()
                    else -> "译文: $text\n\n💡 提示：$userMsg"
                }
            }
        } catch (e: Exception) {
            val errDesc = e.message ?: "网络连接异常"
            return@withContext when (promptType) {
                "word" -> {
                    val dictRes = queryFreeOnlineDictionary(text)
                    "$dictRes\n\n💡 提示：已自动切至权威词典查询（$errDesc）"
                }
                "sentence", "complex_sentence" -> """
                    🎯 【句子基线分析 (离线备用模式)】
                    "$text"
                    
                    🧩 【结构提示】
                    • 核心句子已存入句法本。
                    • 💡 提示：$errDesc
                """.trimIndent()
                else -> "译文: $text\n\n💡 提示：$errDesc"
            }
        }
    }

    suspend fun testConnection(apiKey: String, modelName: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key 不能为空，请先填入有效的 DeepSeek API Key"))
        }
        val model = resolveEffectiveModel(modelName)

        val reqObj = DeepSeekRequest(
            model = model,
            messages = listOf(
                DeepSeekMessage(role = "user", content = "Hello! Please reply with 'OK' to confirm API connection.")
            )
        )

        val reqJson = json.encodeToString(DeepSeekRequest.serializer(), reqObj)
        val requestBody = reqJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val url = "https://api.deepseek.com/chat/completions"

        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val resObj = json.decodeFromString(DeepSeekResponse.serializer(), bodyStr)
                    val reply = resObj.choices?.firstOrNull()?.message?.content?.trim() ?: "OK"
                    Result.success("✅ 连接成功！模型 [$model] 响应正常 (延迟 ${latency}ms)\n回复: \"$reply\"")
                } else {
                    val errorBody = response.body?.string() ?: ""
                    val code = response.code
                    val reason = when {
                        code == 402 || errorBody.contains("balance", ignoreCase = true) || errorBody.contains("insufficient", ignoreCase = true) ->
                            "DeepSeek 账号余额不足或已欠费 (HTTP $code)，请登录平台充值后重试。"
                        code == 401 ->
                            "API Key 无效或未授权 (HTTP 401)，请核对【设置】中的 Key。"
                        code == 400 ->
                            "请求错误 (HTTP 400)。可能账号欠费或模型名称错误。\n详情: ${errorBody.take(100)}"
                        else ->
                            "HTTP $code: ${errorBody.take(100)}"
                    }
                    Result.failure(Exception(reason))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateSmartLocalResponse(text: String, promptType: String): String {
        val trimmed = text.trim()
        return when (promptType) {
            "word" -> queryFreeOnlineDictionary(trimmed)

            "sentence", "complex_sentence" -> """
                🎯 【英文原文】
                "$trimmed"
                
                💡 【提示与建议】
                当前未在【设置】中配置 DeepSeek API Key。在设置中填入 API Key 后，AI 将为你提供精准的中文翻译、长难句结构拆解与核心动词注解。
            """.trimIndent()

            "mindmap" -> "⚠️ 未配置 DeepSeek API Key，请先前往【设置】填入 API Key 即可一键生成！"

            else -> "译文: $trimmed\n（提示：未设置 API Key，请在设置中进行配置）"
        }
    }

    private val builtInDictionary = mapOf(
        "primeval" to "adj. 原始的；远古的",
        "digestion" to "n. 消化；消化能力",
        "connotation" to "n. 涵义；言外之意；隐含意义",
        "cognitive" to "adj. 认知的；感知的；认识的",
        "nuance" to "n. 细微差别；微妙之处",
        "reading" to "n. 阅读；读物 adj. 阅读的",
        "dialogue" to "n. 对话；意见交换",
        "narrative" to "n. 叙述；故事 adj. 叙述的",
        "neural" to "adj. 神经的；神经系统的",
        "pathways" to "n. 路径；神经通路",
        "endurance" to "n. 忍耐力；持久力；耐力",
        "linguistic" to "adj. 语言的；语言学的",
        "intuition" to "n. 直觉；直觉力",
        "authentic" to "adj. 真实的；地道的；可靠的",
        "embedded" to "adj. 嵌入的；植入的",
        "boa" to "n. 蟒蛇；王蛇",
        "constrictor" to "n. 蟒蛇；大蛇",
        "swallow" to "v. 吞下；咽下",
        "prey" to "n. 猎物；受害者",
        "chewing" to "n./v. 咀嚼",
        "pondered" to "v. 沉思；深思熟虑",
        "jungle" to "n. 丛林；密林",
        "succeeded" to "v. 成功；继承"
    )

    private fun queryFreeOnlineDictionary(word: String): String {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return "请输入有效的英文单词"

        // 1. Check built-in vocabulary dictionary
        val builtInMeaning = builtInDictionary[clean]
        if (builtInMeaning != null) {
            return """
                📖 【权威词典释义 (内置离线词库)】
                • 单词: $clean
                • 释义: $builtInMeaning
            """.trimIndent()
        }

        var fullExplanation: String? = null
        var matchedEntry: String = clean

        // 2. Query Baidu Fanyi SUG API first (most complete Chinese explanations without truncation)
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = "kw=$encoded".toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://fanyi.baidu.com/sug")
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val dataArray = root["data"]?.jsonArray
                        if (dataArray != null && dataArray.isNotEmpty()) {
                            var targetItem: kotlinx.serialization.json.JsonObject? = null
                            for (element in dataArray) {
                                val item = element.jsonObject
                                val k = item["k"]?.jsonPrimitive?.content ?: ""
                                if (k.equals(clean, ignoreCase = true)) {
                                    targetItem = item
                                    break
                                }
                            }
                            if (targetItem == null) {
                                targetItem = dataArray[0].jsonObject
                            }
                            val k = targetItem["k"]?.jsonPrimitive?.content ?: clean
                            val v = targetItem["v"]?.jsonPrimitive?.content ?: ""
                            if (v.isNotBlank()) {
                                matchedEntry = k
                                fullExplanation = v
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeepSeekService", "Baidu dictionary query failed", e)
        }

        // 3. Query Youdao Dictionary Suggest API (num=10)
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = "https://dict.youdao.com/suggest?num=10&ver=3.0&doctype=json&cache=false&le=en&q=$encoded"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val data = root["data"]?.jsonObject
                        val entries = data?.get("entries")?.jsonArray
                        if (entries != null && entries.isNotEmpty()) {
                            var targetEntry: kotlinx.serialization.json.JsonObject? = null
                            for (element in entries) {
                                val item = element.jsonObject
                                val entry = item["entry"]?.jsonPrimitive?.content ?: ""
                                if (entry.equals(clean, ignoreCase = true)) {
                                    targetEntry = item
                                    break
                                }
                            }
                            if (targetEntry == null) {
                                targetEntry = entries[0].jsonObject
                            }
                            val explain = targetEntry["explain"]?.jsonPrimitive?.content ?: ""
                            val entry = targetEntry["entry"]?.jsonPrimitive?.content ?: clean

                            if (fullExplanation.isNullOrBlank()) {
                                if (explain.isNotBlank()) {
                                    matchedEntry = entry
                                    fullExplanation = explain
                                }
                            } else if (!explain.endsWith("...") && explain.length > (fullExplanation?.length ?: 0)) {
                                matchedEntry = entry
                                fullExplanation = explain
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeepSeekService", "Youdao dictionary query failed", e)
        }

        if (!fullExplanation.isNullOrBlank()) {
            return """
                📖 【权威在线词典释义】
                • 单词: $matchedEntry
                • 中文释义: $fullExplanation
            """.trimIndent()
        }

        return """
            📖 【词典查询】: $clean
            • 释义: 暂无词条释义
        """.trimIndent()
    }
}
