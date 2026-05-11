package com.interactiveword.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.min

private const val POS_QUIZ_LIMIT = 5

enum class PosQuizEmptyReason {
    NONE,
    NO_WORDS,
    NO_POS_DATA,
    LOAD_ERROR,
}

data class PosQuizQuestion(
    val wordId: Int,
    val word: String,
    val definition: String,
    val correctPos: String,
)

data class PosQuizUiState(
    val isLoading: Boolean = true,
    val questions: List<PosQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerChecked: Boolean = false,
    val correctCount: Int = 0,
    val errorMessage: String? = null,
    val emptyReason: PosQuizEmptyReason = PosQuizEmptyReason.NONE,
) {
    val totalQuestions: Int get() = questions.size
    val isFinished: Boolean get() = totalQuestions > 0 && currentIndex >= totalQuestions
    val currentQuestion: PosQuizQuestion?
        get() = if (currentIndex in questions.indices) questions[currentIndex] else null
}

class PosQuizViewModel(
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    companion object {
        val options = listOf("명사", "동사", "형용사", "부사")
    }

    private val _uiState = MutableStateFlow(PosQuizUiState())
    val uiState: StateFlow<PosQuizUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return
        if (state.isAnswerChecked) return

        val isCorrect = answer == currentQuestion.correctPos
        _uiState.value = state.copy(
            selectedAnswer = answer,
            isAnswerChecked = true,
            correctCount = state.correctCount + if (isCorrect) 1 else 0,
        )
    }

    fun goToNextQuestion() {
        val state = _uiState.value
        if (!state.isAnswerChecked) return

        _uiState.value = state.copy(
            currentIndex = state.currentIndex + 1,
            selectedAnswer = null,
            isAnswerChecked = false,
        )
    }

    fun restartQuiz() {
        loadQuestions()
    }

    fun calculateLocalXp(): Int {
        val correctCount = _uiState.value.correctCount
        val total = _uiState.value.totalQuestions
        if (total == 0) return 0
        val baseXp = correctCount * 10
        val bonusXp = if (total >= 3 && correctCount == total) 10 else 0
        return baseXp + bonusXp
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.value = PosQuizUiState(isLoading = true)
            try {
                val words = wordRepo.getMyWords()
                if (words.isEmpty()) {
                    _uiState.value = PosQuizUiState(
                        isLoading = false,
                        errorMessage = "단어장에 단어를 1회 이상 추가해주세요.",
                        emptyReason = PosQuizEmptyReason.NO_WORDS,
                    )
                    return@launch
                }
                val questions = buildQuestions(words)
                _uiState.value = if (questions.isEmpty()) {
                    PosQuizUiState(
                        isLoading = false,
                        errorMessage = "품사 정보가 있는 저장 단어가 아직 없어요.",
                        emptyReason = PosQuizEmptyReason.NO_POS_DATA,
                    )
                } else {
                    PosQuizUiState(
                        isLoading = false,
                        questions = questions,
                    )
                }
            } catch (e: Throwable) {
                _uiState.value = PosQuizUiState(
                    isLoading = false,
                    errorMessage = when (e) {
                        is HttpException -> when (e.code()) {
                            401 -> "로그인 정보가 만료되어 단어를 불러오지 못했습니다. 다시 로그인해주세요."
                            else -> "서버 응답 오류(${e.code()})로 품사 테스트를 시작할 수 없어요."
                        }
                        is IOException -> "서버에 연결하지 못했습니다. 네트워크 또는 서버 상태를 확인해주세요."
                        else -> "품사 테스트 문제를 불러오지 못했습니다."
                    },
                    emptyReason = PosQuizEmptyReason.LOAD_ERROR,
                )
            }
        }
    }

    private fun buildQuestions(words: List<WordCard>): List<PosQuizQuestion> {
        return words
            .mapNotNull { card ->
                val definition = card.definition?.trim().orEmpty()
                val normalizedPos = normalizePos(card.pos)
                if (definition.isBlank() || normalizedPos == null) return@mapNotNull null

                PosQuizQuestion(
                    wordId = card.id,
                    word = card.koreanWord,
                    definition = definition,
                    correctPos = normalizedPos,
                )
            }
            .shuffled()
            .take(min(POS_QUIZ_LIMIT, words.size))
    }

    private fun normalizePos(raw: String?): String? {
        val pos = raw?.trim().orEmpty()
        if (pos.isBlank()) return null
        return when {
            "명사" in pos -> "명사"
            "동사" in pos -> "동사"
            "형용사" in pos -> "형용사"
            "부사" in pos -> "부사"
            else -> null
        }
    }
}
