package com.example.shiyu.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

/**
 * 系统首选 TTS 语音朗读服务 (System Preferred TTS Service)
 * 1. 【系统 TTS 优先】：默认直接调用手机系统自带/首选的 Android 原生 TextToSpeech 引擎，中国大陆完全无网路墙问题。
 * 2. 【智能长句分段】：对于长难句，自动按标点切割并使用 QUEUE_ADD 连续顺畅播放。
 * 3. 【无缝初始化等待】：若初始化尚未完成，自动将朗读指令入队，初始化就绪后立即播放。
 * 4. 【国内在线备用】：若系统缺乏英文语音包，自动平滑切至国内直连的有道美音引擎 (Youdao DictVoice)。
 */
class TtsService(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isNativeInitialized = false
    private var isNativeAvailable = false
    private var pendingTextToSpeak: String? = null

    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TtsService", "Failed to construct native TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        isNativeInitialized = true
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 某些设备可能缺少 US 语音包，尝试通用 ENGLISH
                val engResult = tts?.setLanguage(Locale.ENGLISH)
                if (engResult == TextToSpeech.LANG_MISSING_DATA || engResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("TtsService", "Native TTS missing English language pack")
                    isNativeAvailable = false
                } else {
                    tts?.setSpeechRate(0.95f)
                    isNativeAvailable = true
                    Log.d("TtsService", "Native Android TextToSpeech initialized with Locale.ENGLISH")
                }
            } else {
                tts?.setSpeechRate(0.95f)
                isNativeAvailable = true
                Log.d("TtsService", "Native Android TextToSpeech initialized successfully with Locale.US")
            }
        } else {
            Log.w("TtsService", "Native TextToSpeech init failed with status code: $status")
            isNativeAvailable = false
        }

        // 处理之前初始化未完成时触发的等待朗读
        val pending = pendingTextToSpeak
        pendingTextToSpeak = null
        if (!pending.isNullOrBlank()) {
            speakInternal(pending, null)
        }
    }

    /**
     * 执行朗读：系统 TTS 优先！
     */
    fun speak(text: String, onStatusMsg: ((String) -> Unit)? = null) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        stop()

        if (!isNativeInitialized) {
            pendingTextToSpeak = cleanText
            mainHandler.post { onStatusMsg?.invoke("正在初始化系统朗读引擎...") }
            return
        }

        speakInternal(cleanText, onStatusMsg)
    }

    private fun speakInternal(text: String, onStatusMsg: ((String) -> Unit)?) {
        // 1. 如果系统 TTS 可用，直接调用手机首选 TTS 引擎朗读
        if (isNativeAvailable && tts != null) {
            val isSuccess = speakViaNativeTts(text)
            if (isSuccess) {
                mainHandler.post { onStatusMsg?.invoke("正在朗读...") }
                return
            }
        }

        // 2. 如果系统原生 TTS 不可用（无语音包等），降级到国内直连的 Youdao 在线发音
        speakViaYoudaoOnline(text, onStatusMsg)
    }

    /**
     * 使用手机原生 System TTS 朗读（支持长句按标点分句连续朗读）
     */
    private fun speakViaNativeTts(text: String): Boolean {
        return try {
            val engine = tts ?: return false
            val maxLen = TextToSpeech.getMaxSpeechInputLength()
            
            if (text.length <= maxLen && !text.contains("\n")) {
                val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shiyu_tts_single")
                res == TextToSpeech.SUCCESS
            } else {
                // 长文本/多句分割，使用 QUEUE_ADD 连续顺畅播放
                val sentences = text.split(Regex("(?<=[.!?;\\n])\\s+")).filter { it.isNotBlank() }
                if (sentences.isEmpty()) return false

                engine.speak(sentences[0], TextToSpeech.QUEUE_FLUSH, null, "shiyu_tts_0")
                for (i in 1 until sentences.size) {
                    engine.speak(sentences[i], TextToSpeech.QUEUE_ADD, null, "shiyu_tts_$i")
                }
                true
            }
        } catch (e: Exception) {
            Log.e("TtsService", "speakViaNativeTts exception", e)
            false
        }
    }

    /**
     * 国内直连的在线音源（有道美音 / 英音）
     */
    private fun speakViaYoudaoOnline(text: String, onStatusMsg: ((String) -> Unit)?) {
        thread {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                // 有道美音 (type=2) / 英音 (type=1)，中国大陆直连，无网络障碍
                val sources = listOf(
                    "https://dict.youdao.com/dictvoice?audio=$encodedText&type=2",
                    "https://dict.youdao.com/dictvoice?audio=$encodedText&type=1"
                )

                var played = false
                for (url in sources) {
                    try {
                        mainHandler.post { onStatusMsg?.invoke("正在在线朗读...") }
                        stopMediaPlayer()

                        val mp = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(url)
                            prepare()
                            start()
                            setOnCompletionListener {
                                stopMediaPlayer()
                                mainHandler.post { onStatusMsg?.invoke("朗读完成") }
                            }
                            setOnErrorListener { _, _, _ ->
                                stopMediaPlayer()
                                true
                            }
                        }
                        mediaPlayer = mp
                        played = true
                        break
                    } catch (e: Exception) {
                        Log.w("TtsService", "Youdao voice stream failed: $url", e)
                    }
                }

                if (!played) {
                    mainHandler.post {
                        Toast.makeText(context, "无法启动朗读，请检查网络或在系统设置中下载英文 TTS 语音包", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TtsService", "speakViaYoudaoOnline error", e)
            }
        }
    }

    /**
     * 引导用户打开系统 TTS 设置（供用户选择系统默认引擎如讯飞、小米TTS、谷歌TTS、华为TTS等）
     */
    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "无法直接打开系统TTS设置页面", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stop() {
        try {
            if (isNativeAvailable) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e("TtsService", "Error stopping native TTS", e)
        }
        stopMediaPlayer()
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("TtsService", "Error stopping MediaPlayer", e)
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsService", "Error shutting down TTS", e)
        }
        tts = null
        isNativeInitialized = false
        isNativeAvailable = false
    }
}
