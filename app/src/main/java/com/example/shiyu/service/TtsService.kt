package com.example.shiyu.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
 * 智能混合语音朗读服务 (Hybrid TTS Service)
 * 针对中国大陆手机（如小米、华为、OPPO、vivo 等）原生 Google TTS 易缺失的情况：
 * 1. 【优先 Fallback 在线高清源】：联网时优先使用标准英/美音高清语音引擎，无需系统安装 TTS 语音包，100% 保证发音。
 * 2. 【智能多源重试】：主在线源 -> 备用在线源 -> 本地离线引擎。
 * 3. 【离线自动降级】：无网络时全自动切回 Android 本地 TextToSpeech 离线朗读，确保离线可用。
 */
class TtsService(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isNativeInitialized = false
    private var isNativeAvailable = false
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
                Log.w("TtsService", "Native TTS missing US English language pack")
                isNativeAvailable = false
            } else {
                tts?.setSpeechRate(0.95f)
                isNativeAvailable = true
                Log.d("TtsService", "Native Android TextToSpeech initialized successfully")
            }
        } else {
            Log.w("TtsService", "Native TextToSpeech init failed with status code: $status")
            isNativeAvailable = false
        }
    }

    /**
     * 执行朗读：优先使用网络高清源，离线或失败时自动降级到本地离线引擎
     */
    fun speak(text: String, onStatusMsg: ((String) -> Unit)? = null) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        stop()

        val isOnline = isNetworkConnected()

        if (isOnline) {
            // 联网状态：优先使用在线发音（确保中国大陆设备100%可流畅朗读标准发音）
            playOnlineStreamWithFallback(cleanText, onStatusMsg)
        } else {
            // 离线状态：尝试使用本地离线原生 TextToSpeech 引擎
            speakOfflineNative(cleanText, onStatusMsg)
        }
    }

    /**
     * 在线发音 (支持多 API 源备用 + 离线降级)
     */
    private fun playOnlineStreamWithFallback(text: String, onStatusMsg: ((String) -> Unit)?) {
        thread {
            val encodedText = try {
                URLEncoder.encode(text, "UTF-8")
            } catch (e: Exception) {
                text
            }

            // 支持两个备用在线源（有道美音 / 词典语音）
            val sources = listOf(
                "https://dict.youdao.com/dictvoice?audio=$encodedText&type=2",
                "https://dict.youdao.com/dictvoice?audio=$encodedText&type=1"
            )

            var playedSuccess = false

            for (audioUrl in sources) {
                try {
                    mainHandler.post { onStatusMsg?.invoke("正在朗读...") }
                    stopMediaPlayer()

                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(audioUrl)
                        prepare()
                        start()
                        setOnCompletionListener {
                            stopMediaPlayer()
                        }
                        setOnErrorListener { _, _, _ ->
                            stopMediaPlayer()
                            true
                        }
                    }
                    mediaPlayer = mp
                    playedSuccess = true
                    Log.d("TtsService", "Playing online voice stream from $audioUrl")
                    break
                } catch (e: Exception) {
                    Log.w("TtsService", "Online source failed: $audioUrl, error: ${e.message}")
                }
            }

            // 若在线 API 均失败，降级使用本地离线引擎
            if (!playedSuccess) {
                mainHandler.post {
                    Log.w("TtsService", "Online audio failed, falling back to local offline TTS")
                    speakOfflineNative(text, onStatusMsg)
                }
            }
        }
    }

    /**
     * 离线本地原生 TTS 朗读
     */
    private fun speakOfflineNative(text: String, onStatusMsg: ((String) -> Unit)?) {
        if (isNativeAvailable && tts != null) {
            val res = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shiyu_tts_id")
            if (res == TextToSpeech.SUCCESS) {
                Log.d("TtsService", "Speaking via native offline TTS: $text")
                return
            }
        }

        // 离线且无本地引擎时的友善提示
        mainHandler.post {
            Toast.makeText(
                context,
                "当前为离线模式，且系统未检测到英文语音包。建议连接网络或在系统设置中安装 TTS 引擎。",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 检查网络连接状态
     */
    private fun isNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // 默认尝试联网
        }
    }

    /**
     * 引导用户打开系统 TTS 设置（供用户手动选择/安装语音引擎）
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
