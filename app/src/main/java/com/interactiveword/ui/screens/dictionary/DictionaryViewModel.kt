package com.interactiveword.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val result: DictionaryResult? = null,
    val isLoading: Boolean = false,
    val addedSuccess: Boolean = false,
)

@OptIn(FlowPreview::class)
class DictionaryViewModel(
    private val repo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        // 500ms 디바운스 후 검색
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
        _uiState.value = _uiState.value.copy(query = q, result = null)
        queryFlow.value = q
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val card = repo.createWord(query, source = "dictionary", dryRun = true)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = DictionaryResult(
                    word       = card.koreanWord,
                    pos        = card.pos,
                    definition = card.definition,
                ),
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun addToCollection(word: String) {
        viewModelScope.launch {
            try {
                repo.createWord(word, source = "dictionary")
                _uiState.value = _uiState.value.copy(addedSuccess = true)
            } catch (_: Exception) {}
        }
    }
}
