package com.interactiveword.ui.screens.wordcard

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WordCardUiState(
    val card: WordCard? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class WordCardViewModel(
    private val repo: WordRepository = WordRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordCardUiState())
    val uiState: StateFlow<WordCardUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun loadCard(wordId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val card = repo.getWord(wordId)
                _uiState.value = WordCardUiState(card = card, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun playTts() {
        val path = _uiState.value.card?.ttsAudioPath
        val url = RetrofitClient.resolveStaticUrl(path)

        if (url == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "TTS 파일 경로가 없습니다.")
            return
        }

        playUrl(url, "TTS 재생 실패")
    }

    fun playExampleTts(path: String?) {
        val url = RetrofitClient.resolveStaticUrl(path)

        if (url == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "예문 TTS 파일 경로가 없습니다.")
            return
        }

        playUrl(url, "예문 TTS 재생 실패")
    }

    private fun playUrl(url: String, errorPrefix: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "$errorPrefix: ${e.message}"
            )
        }
    }

    fun startPronunciationPractice() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "발음 연습 기능은 준비 중입니다."
        )
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}
