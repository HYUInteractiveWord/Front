package com.interactiveword.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.repository.ScanRepository
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScanType { MIC, MEDIA }

data class ScanUiState(
    val isRecording: Boolean = false,
    val scanType: ScanType = ScanType.MIC,
    val detectedWords: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ScanViewModel(
    private val scanRepo: ScanRepository = ScanRepository(),
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun startMicRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true, scanType = ScanType.MIC)
        // TODO: AudioRecord로 마이크 녹음 시작 → 5초 후 자동 전송
        viewModelScope.launch {
            delay(5000)
            stopRecording()
        }
    }

    fun startMediaCapture() {
        _uiState.value = _uiState.value.copy(isRecording = true, scanType = ScanType.MEDIA)
        // TODO: MediaProjection 권한 요청 후 AudioPlaybackCapture 시작
        viewModelScope.launch {
            delay(5000)
            stopRecording()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRecording = false, isLoading = true)
            try {
                // TODO: 실제 녹음된 bytes를 서버로 전송
                // val result = scanRepo.uploadAudio(audioBytes, scanType)
                // _uiState.value = _uiState.value.copy(detectedWords = result, isLoading = false)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun addWordToCollection(word: String) {
        viewModelScope.launch {
            try {
                wordRepo.createWord(word, source = "scan")
                dismissWord(word)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismissWord(word: String) {
        _uiState.value = _uiState.value.copy(
            detectedWords = _uiState.value.detectedWords.filter { it != word },
        )
    }
}
