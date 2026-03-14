package com.example.mvpapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mvpapp.VoskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpeechUiState(
    val status: AppStatus = AppStatus.LOADING_MODEL,
    val loadingMessage: String = "جاري تحميل الموديلات...",
    val partialText: String = "",
    val fullTranscript: String = "",
    val isRecording: Boolean = false,
    val currentLanguage: AppLanguage = AppLanguage.ARABIC,
    val errorMessage: String? = null
)

enum class AppStatus {
    LOADING_MODEL,
    READY,
    RECORDING,
    STOPPED,
    ERROR
}

class SpeechViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    private val voskManager = VoskManager(application)

    init {
        setupCallbacks()
        voskManager.loadModels()
    }

    private fun setupCallbacks() {

        voskManager.onLoadingProgress = { message ->
            _uiState.update { it.copy(status = AppStatus.LOADING_MODEL, loadingMessage = message) }
        }

        voskManager.onModelsReady = {
            _uiState.update { it.copy(status = AppStatus.READY, loadingMessage = "") }
        }

        voskManager.onPartialResult = { partial ->
            _uiState.update { it.copy(partialText = partial) }
        }

        voskManager.onResult = { text ->
            _uiState.update { state ->
                state.copy(
                    fullTranscript = state.fullTranscript + "$text\n",
                    partialText = ""
                )
            }
        }

        voskManager.onError = { error ->
            _uiState.update { it.copy(status = AppStatus.ERROR, errorMessage = error) }
        }
    }

    // ── Actions ──────────────────────────────────────────

    fun startRecording() {
        voskManager.startListening()
        _uiState.update { it.copy(status = AppStatus.RECORDING, isRecording = true) }
    }

    fun stopRecording() {
        voskManager.stopListening()
        _uiState.update { it.copy(
            status = AppStatus.STOPPED,
            isRecording = false,
            partialText = ""
        )}
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) stopRecording() else startRecording()
    }

    // ── Switch اللغة ─────────────────────────────────────
    fun switchLanguage(language: AppLanguage) {
        val wasRecording = _uiState.value.isRecording
        _uiState.update { it.copy(currentLanguage = language) }
        voskManager.switchLanguage(language, wasRecording)
    }

    fun clearTranscript() {
        _uiState.update { it.copy(fullTranscript = "", partialText = "") }
    }

    override fun onCleared() {
        super.onCleared()
        voskManager.destroy()
    }
}
