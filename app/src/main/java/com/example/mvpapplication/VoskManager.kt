package com.example.mvpapp

import android.content.Context
import android.util.Log
import com.example.mvpapplication.AppLanguage
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File



class VoskManager(private val context: Context) {

    private var arabicModel: Model? = null
    private var englishModel: Model? = null
    private var speechService: SpeechService? = null
    private var currentLanguage = AppLanguage.ARABIC

    companion object {
        private const val TAG = "VoskManager"
    }

    var onPartialResult: ((String) -> Unit)? = null
    var onResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onModelsReady: (() -> Unit)? = null
    var onLoadingProgress: ((String) -> Unit)? = null

    // ════════════════════════════════════════════════════
    // تحميل الموديلين
    // ════════════════════════════════════════════════════
    fun loadModels() {
        Thread {
            try {
                // ── عربي ──────────────────────────────
                onLoadingProgress?.invoke("جاري تحميل الموديل العربي...")
                val arPath = copyModelFromAssets("model-ar")
                Log.d(TAG, "Arabic model path: $arPath")
                arabicModel = Model(arPath)
                Log.d(TAG, "✅ Arabic model loaded")

                // ── إنجليزي ───────────────────────────
                onLoadingProgress?.invoke("جاري تحميل الموديل الإنجليزي...")
                val enPath = copyModelFromAssets("model-en")
                Log.d(TAG, "English model path: $enPath")
                englishModel = Model(enPath)
                Log.d(TAG, "✅ English model loaded")

                onModelsReady?.invoke()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Load error: ${e.message}")
                onError?.invoke("فشل التحميل: ${e.message}")
            }
        }.start()
    }

    // ════════════════════════════════════════════════════
    // نسخ الموديل من assets للـ filesDir
    // ════════════════════════════════════════════════════
    private fun copyModelFromAssets(modelName: String): String {
        val outDir = File(context.filesDir, modelName)

        // لو الموديل اتنسخ قبل كده متنسخش تاني
        if (outDir.exists() && outDir.listFiles()?.isNotEmpty() == true) {
            Log.d(TAG, "Model already exists: ${outDir.absolutePath}")
            return outDir.absolutePath
        }

        outDir.mkdirs()
        copyAssetFolder(modelName, outDir)
        Log.d(TAG, "Model copied to: ${outDir.absolutePath}")
        return outDir.absolutePath
    }

    private fun copyAssetFolder(assetPath: String, outDir: File) {
        val assets = context.assets.list(assetPath) ?: return
        for (asset in assets) {
            val subAssetPath = "$assetPath/$asset"
            val outFile = File(outDir, asset)
            val subList = context.assets.list(subAssetPath)
            if (subList != null && subList.isNotEmpty()) {
                // فولدر → ادخل جواه
                outFile.mkdirs()
                copyAssetFolder(subAssetPath, outFile)
            } else {
                // ملف → انسخه
                context.assets.open(subAssetPath).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    // Switch اللغة
    // ════════════════════════════════════════════════════
    fun switchLanguage(language: AppLanguage, wasRecording: Boolean) {
        currentLanguage = language
        if (wasRecording) {
            stopListening()
            startListening()
        }
    }

    // ════════════════════════════════════════════════════
    // ابدأ الاستماع
    // ════════════════════════════════════════════════════
    fun startListening() {
        val model = if (currentLanguage == AppLanguage.ARABIC) arabicModel else englishModel
        if (model == null) {
            onError?.invoke("الموديل مش محمل لسه")
            return
        }
        try {
            Log.d(TAG, "🎙️ Starting listening with language: $currentLanguage")
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(listener)
            Log.d(TAG, "🎙️ SpeechService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Start error: ${e.message}")
            onError?.invoke("خطأ: ${e.message}")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
    }

    fun destroy() {
        stopListening()
        arabicModel?.close()
        englishModel?.close()
    }

    // ════════════════════════════════════════════════════
    // Listener
    // ════════════════════════════════════════════════════
    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            Log.d(TAG, "📝 Partial: $hypothesis")
            onPartialResult?.invoke(parseJson(hypothesis, "partial"))
        }
        override fun onResult(hypothesis: String?) {
            Log.d(TAG, "✅ Result: $hypothesis")
            val text = parseJson(hypothesis, "text")
            if (text.isNotEmpty()) onResult?.invoke(text)
        }
        override fun onFinalResult(hypothesis: String?) {
            Log.d(TAG, "🏁 Final: $hypothesis")
            val text = parseJson(hypothesis, "text")
            if (text.isNotEmpty()) onResult?.invoke(text)
        }
        override fun onError(exception: Exception?) {
            Log.e(TAG, "❌ Error: ${exception?.message}")
            onError?.invoke("خطأ في التعرف: ${exception?.message}")
        }
        override fun onTimeout() {
            Log.d(TAG, "⏰ Timeout")
        }
    }

    private fun parseJson(json: String?, key: String): String {
        return try {
            JSONObject(json ?: return "").optString(key, "")
        } catch (e: Exception) { "" }
    }
}