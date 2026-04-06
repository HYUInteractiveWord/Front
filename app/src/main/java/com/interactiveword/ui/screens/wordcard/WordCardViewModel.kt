package com.interactiveword.ui.screens.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WordCardUiState(
    val card: WordCard? = null,
    val isLoading: Boolean = false,
)

class WordCardViewModel(
    private val repo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordCardUiState())
    val uiState: StateFlow<WordCardUiState> = _uiState.asStateFlow()

    fun loadCard(wordId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val card = repo.getWord(wordId)
                _uiState.value = WordCardUiState(card = card)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun playTts() {
        // TODO: tts_audio_path로 MediaPlayer 재생
    }

    fun startPronunciationPractice() {
        // TODO: 마이크 녹음 시작 → 백엔드 /pronunciation/submit
    }
}
