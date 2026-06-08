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

enum class SortOrder {
    NEWEST, SCORE
}

data class CollectionUiState(
    val words: List<WordCard> = emptyList(),
    val filteredWords: List<WordCard> = emptyList(),
    val maxSlots: Int = 20,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val userId: Int? = null,
    val searchQuery: String = "",
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
                
                val sortedWords = sortWords(words, _uiState.value.sortOrder)

                _uiState.value = _uiState.value.copy(
                    words = sortedWords,
                    maxSlots = user.maxWordSlots,
                    isLoading = false,
                    userId = user.id
                )
                applyFilter()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.searchQuery.trim()
        val allWords = _uiState.value.words
        
        val filtered = if (query.isEmpty()) {
            allWords
        } else {
            allWords.filter { card ->
                // 한국어 단어 검색
                val wordMatch = card.koreanWord.contains(query, ignoreCase = true)
                
                // 뜻 부분 검색 (the plants in forest / 나무 식으로 되어 있을 때 슬래시 앞부분)
                // 현재 설명에 따르면 'wood / the plants in forest' 식으로 저장됨.
                // 한국어 뜻이 뒤에 있는 경우: 'wood / 나무' -> query가 '나무'인 경우
                val definitionMatch = card.definitionTranslated?.let { def ->
                    val mainDef = if (def.contains("/")) {
                        def.split("/").first().trim()
                    } else {
                        def.trim()
                    }
                    mainDef.contains(query, ignoreCase = true)
                } ?: false
                
                wordMatch || definitionMatch
            }
        }
        _uiState.value = _uiState.value.copy(filteredWords = filtered)
    }

    fun setSortOrder(order: SortOrder) {
        if (_uiState.value.sortOrder == order) return
        val sortedAll = sortWords(_uiState.value.words, order)
        _uiState.value = _uiState.value.copy(sortOrder = order, words = sortedAll)
        applyFilter()
    }

    private fun sortWords(list: List<WordCard>, order: SortOrder): List<WordCard> {
        return when (order) {
            SortOrder.NEWEST -> list.sortedByDescending { it.id }
            SortOrder.SCORE -> list.sortedByDescending { it.wordPoint }
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