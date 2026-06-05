package com.interactiveword.ui.screens.collection

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.UserRepository
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CollectionUiState(
    val words: List<WordCard> = emptyList(),
    val maxSlots: Int = 20,
    val isLoading: Boolean = false,
)

class CollectionViewModel(
    private val repo: WordRepository = WordRepository(),
    private val userRepo: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    //콜렉션 화면용 미디어 플레이어
    private var mediaPlayer: MediaPlayer? = null

    init { loadWords() }

    fun loadWords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = userRepo.getMe()
                val words = repo.getMyWords()
                _uiState.value = _uiState.value.copy(
                    words = words,
                    maxSlots = user.maxWordSlots,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteWord(wordId: Int) {
        viewModelScope.launch {
            repo.deleteWord(wordId)
            loadWords()
        }
    }

    // 한국어 표제어 재생
    fun playTts(path: String?) {
        val url = RetrofitClient.resolveStaticUrl(path) ?: return
        playUrl(url)
    }

    // 번역본 뜻 재생
    fun playTransTts(path: String?) {
        val url = RetrofitClient.resolveStaticUrl(path) ?: return
        playUrl(url)
    }

    private fun playUrl(url: String) {
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
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}