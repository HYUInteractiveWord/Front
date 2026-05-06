package com.interactiveword.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DictionaryResult(
    val word: String,
    val pos: String?,
    val definition: String?,
)

data class DictionaryUiState(
    val query: String = "",
    val candidates: List<DictionaryResult> = emptyList(),
    val isLoading: Boolean = false,
    val addedSuccess: Boolean = false,
    val addedWords: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

class DictionaryViewModel(
    private val repo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(
            query = q,
            candidates = emptyList(),
            errorMessage = null,
        )
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
        )

        try {
            val response = repo.searchDictionary(query)

            val candidateResults = response.candidates.map { (word, info) ->
                DictionaryResult(
                    word = word,
                    pos = info.pos,
                    definition = info.definition,
                )
            }

            val fallbackResult = response.word
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    DictionaryResult(
                        word = it,
                        pos = response.pos,
                        definition = response.definition,
                    )
                }

            val results = if (candidateResults.isNotEmpty()) {
                candidateResults
            } else {
                listOfNotNull(fallbackResult)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                candidates = results,
                errorMessage = null,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                candidates = emptyList(),
                errorMessage = e.message ?: "검색에 실패했습니다.",
            )
        }
    }

    fun addToCollection(word: String) {
        viewModelScope.launch {
            try {
                repo.createWord(word, source = "dictionary")
                _uiState.value = _uiState.value.copy(
                    addedWords = _uiState.value.addedWords + word,
                    addedSuccess = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "단어장 추가에 실패했습니다.",
                )
            }
        }
    }

    fun searchNow() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            search(query)
        }
    }
}