package com.interactiveword.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
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
    val addedWords: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
class DictionaryViewModel(
    private val repo: WordRepository = WordRepository(),
    private val api: com.interactiveword.data.api.ApiService = RetrofitClient.api,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .filter { it.isNotBlank() }
                .collectLatest { query ->
                    search(query)
                }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(query = q, candidates = emptyList())
        queryFlow.value = q
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val response = api.searchDictionary(query)
            val candidates = response.candidates.map { (word, info) ->
                DictionaryResult(word = word, pos = info.pos, definition = info.definition)
            }
            _uiState.value = _uiState.value.copy(isLoading = false, candidates = candidates)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, candidates = emptyList())
        }
    }

    fun addToCollection(word: String) {
        viewModelScope.launch {
            try {
                repo.createWord(word, source = "dictionary")
                _uiState.value = _uiState.value.copy(
                    addedWords = _uiState.value.addedWords + word
                )
            } catch (_: Exception) {}
        }
    }
}
