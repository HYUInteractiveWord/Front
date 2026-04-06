package com.interactiveword.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.WordCard
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init { loadWords() }

    fun loadWords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val words = repo.getMyWords()
                _uiState.value = _uiState.value.copy(words = words, isLoading = false)
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
}
