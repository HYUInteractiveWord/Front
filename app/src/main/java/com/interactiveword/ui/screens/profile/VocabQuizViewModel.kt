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

private const val VOCAB_QUIZ_LIMIT = 5

enum class VocabQuizEmptyReason {
    NONE,
    NO_WORDS,
    NO_DEFINITION_DATA,
    LOAD_ERROR,
}

data class VocabQuizQuestion(
    val wordId: Int,
    val definition: String,
    val correctWord: String,
    val correctPos: String,
    val options: List<String>,
)

data class VocabQuizUiState(
    val isLoading: Boolean = true,
    val questions: List<VocabQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerChecked: Boolean = false,
    val correctCount: Int = 0,
    val errorMessage: String? = null,
    val emptyReason: VocabQuizEmptyReason = VocabQuizEmptyReason.NONE,
) {
    val totalQuestions: Int get() = questions.size
    val isFinished: Boolean get() = totalQuestions > 0 && currentIndex >= totalQuestions
    val currentQuestion: VocabQuizQuestion?
        get() = if (currentIndex in questions.indices) questions[currentIndex] else null
}

private data class VocabCandidate(
    val wordId: Int,
    val word: String,
    val definition: String,
    val pos: String,
)

class VocabQuizViewModel(
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabQuizUiState())
    val uiState: StateFlow<VocabQuizUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return
        if (state.isAnswerChecked) return

        val isCorrect = answer == currentQuestion.correctWord
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
            _uiState.value = VocabQuizUiState(isLoading = true)
            try {
                val words = wordRepo.getMyWords()
                if (words.isEmpty()) {
                    _uiState.value = VocabQuizUiState(
                        isLoading = false,
                        errorMessage = "단어장에 단어를 1회 이상 추가해주세요.",
                        emptyReason = VocabQuizEmptyReason.NO_WORDS,
                    )
                    return@launch
                }

                val questions = buildQuestions(words)
                _uiState.value = if (questions.isEmpty()) {
                    VocabQuizUiState(
                        isLoading = false,
                        errorMessage = "뜻 정보가 있는 저장 단어가 아직 없어요.",
                        emptyReason = VocabQuizEmptyReason.NO_DEFINITION_DATA,
                    )
                } else {
                    VocabQuizUiState(
                        isLoading = false,
                        questions = questions,
                    )
                }
            } catch (e: Throwable) {
                _uiState.value = VocabQuizUiState(
                    isLoading = false,
                    errorMessage = when (e) {
                        is HttpException -> when (e.code()) {
                            401 -> "로그인 정보가 만료되어 단어를 불러오지 못했습니다. 다시 로그인해주세요."
                            else -> "서버 응답 오류(${e.code()})로 단어 뜻 테스트를 시작할 수 없어요."
                        }
                        is IOException -> "서버에 연결하지 못했습니다. 네트워크 또는 서버 상태를 확인해주세요."
                        else -> "단어 뜻 테스트 문제를 불러오지 못했습니다."
                    },
                    emptyReason = VocabQuizEmptyReason.LOAD_ERROR,
                )
            }
        }
    }

    private fun buildQuestions(words: List<WordCard>): List<VocabQuizQuestion> {
        val candidates = words.mapNotNull { card ->
            val definition = card.definition?.trim().orEmpty()
            val normalizedPos = normalizePos(card.pos)
            if (definition.isBlank() || normalizedPos == null) return@mapNotNull null

            VocabCandidate(
                wordId = card.id,
                word = card.koreanWord.trim(),
                definition = definition,
                pos = normalizedPos,
            )
        }

        return candidates
            .shuffled()
            .take(min(VOCAB_QUIZ_LIMIT, candidates.size))
            .map { candidate ->
                VocabQuizQuestion(
                    wordId = candidate.wordId,
                    definition = candidate.definition,
                    correctWord = candidate.word,
                    correctPos = candidate.pos,
                    options = buildOptions(correct = candidate, candidates = candidates),
                )
            }
    }

    private fun buildOptions(
        correct: VocabCandidate,
        candidates: List<VocabCandidate>,
    ): List<String> {
        val samePosWords = candidates
            .filter { it.word != correct.word && it.pos == correct.pos }
            .map { it.word }
            .distinct()
            .shuffled()

        val otherSavedWords = candidates
            .filter { it.word != correct.word && it.pos != correct.pos }
            .map { it.word }
            .distinct()
            .shuffled()

        val fallbackWords = (fallbackWordPool[correct.pos].orEmpty() + fallbackWordPool.values.flatten())
            .filter { it != correct.word }
            .distinct()
            .shuffled()

        val wrongOptions = buildList {
            addDistinctWords(this, samePosWords, 3)
            addDistinctWords(this, otherSavedWords, 3)
            addDistinctWords(this, fallbackWords, 3)
        }.take(3)

        return (wrongOptions + correct.word)
            .distinct()
            .shuffled()
    }

    private fun addDistinctWords(
        target: MutableList<String>,
        source: List<String>,
        limit: Int,
    ) {
        source.forEach { word ->
            if (target.size >= limit) return
            if (word !in target) {
                target += word
            }
        }
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

    private companion object {
        val fallbackWordPool = mapOf(
            "명사" to listOf("연필", "강아지", "도서관", "바다", "시계"),
            "동사" to listOf("걷다", "웃다", "먹다", "배우다", "기다리다"),
            "형용사" to listOf("조용하다", "따뜻하다", "어렵다", "행복하다", "부드럽다"),
            "부사" to listOf("빠르게", "갑자기", "매우", "조용히", "자주"),
        )
    }
}
