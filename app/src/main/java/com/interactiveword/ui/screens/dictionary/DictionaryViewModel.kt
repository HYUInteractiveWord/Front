package com.interactiveword.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.repository.UserRepository
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.interactiveword.util.WordCardPointManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.interactiveword.ui.components.XpManager
import com.interactiveword.ui.components.AppNotification
import com.interactiveword.ui.components.NotiType
import com.interactiveword.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import com.interactiveword.ui.theme.BrandGreenLight

data class DictionaryResult(
    val word: String,
    val pos: String?,
    val definition: String?,
    val definitionTranslated: String? = null,
)

data class DictionaryUiState(
    val query: String = "",
    val candidates: List<DictionaryResult> = emptyList(),
    val isLoading: Boolean = false,
    val addedSuccess: Boolean = false,
    val addedWords: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val isSlotFull: Boolean = false,
)

class DictionaryViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: WordRepository = WordRepository(),
    private val userRepo: UserRepository = UserRepository(),
) : AndroidViewModel(application) {
    private val context = getApplication<Application>()

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
            val user = userRepo.getMe()
            val currentWords = repo.getMyWords()
            val isFull = currentWords.size >= user.maxWordSlots

            val response = repo.searchDictionary(query)
            // ... (rest of search logic remains same but uses isFull)
            val candidateResults = response.candidates.map { (word, info) ->
                DictionaryResult(
                    word = word,
                    pos = info.pos,
                    definition = info.definition,
                    definitionTranslated = info.definitionTranslated,
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
                isSlotFull = isFull
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
                val newCard = repo.createWord(word, source = "dictionary")
                // 💡 신규 추가된 단어 ID를 미확인 목록에 등록
                WordCardPointManager.addUnseenWords(context, listOf(newCard.id))

                // 알림 추가
                XpManager.emitNotification(
                    AppNotification(
                        type = NotiType.NEW_WORD,
                        message = context.getString(R.string.noti_new_word_added, word),
                        color = BrandGreenLight,
                        icon = Icons.Default.MenuBook
                    )
                )

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

    fun clearAddedSuccess() {
        _uiState.value = _uiState.value.copy(
            addedSuccess = false
        )
    }
}