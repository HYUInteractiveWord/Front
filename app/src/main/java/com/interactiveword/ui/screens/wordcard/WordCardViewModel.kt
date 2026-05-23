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
import java.io.File

data class WordCardUiState(
    val card: WordCard? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // 💡 추가됨: 발음 평가 관련 상태
    val isEvaluating: Boolean = false,
    val evalScore: Float? = null,
    val isNewBest: Boolean = false,
    val xpGained: Int? = null,
    val evalGraphs: Map<String, String>? = null
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
                _uiState.value = _uiState.value.copy(card = card, isLoading = false)
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

    fun submitPronunciation(audioFile: File) {
        val currentCard = _uiState.value.card
        if (currentCard == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "단어 정보가 없습니다.")
            return
        }

        viewModelScope.launch {
            // 평가 진행 중 상태
            _uiState.value = _uiState.value.copy(isEvaluating = true, errorMessage = null)

            try {
                // 백엔드로 파일 전송 및 평가 요청
                val result = repo.submitPronunciation(currentCard, audioFile)

                // 결과 반영
                _uiState.value = _uiState.value.copy(
                    isEvaluating = false,
                    evalScore = result.score,
                    isNewBest = result.isNewBest,
                    xpGained = result.xpGained,
                    evalGraphs = result.graphs
                )

                // 신기록일 경우 UI 상의 단어 카드 최고 점수 반영
                if (result.isNewBest) {
                    val updatedCard = currentCard.copy(bestScore = result.score)
                    _uiState.value = _uiState.value.copy(card = updatedCard)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEvaluating = false,
                    errorMessage = "평가 실패: ${e.message}"
                )
            }
        }
    }

    // 평가 결과 초기화
    fun clearEvaluation() {
        _uiState.value = _uiState.value.copy(
            evalScore = null,
            isNewBest = false,
            xpGained = null,
            evalGraphs = null
        )
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}
