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
import kotlin.random.Random

private const val VOCAB_QUIZ_LIMIT = 5

enum class VocabQuizEmptyReason {
    NONE,
    NO_WORDS,
    NO_DEFINITION_DATA,
    LOAD_ERROR,
}

enum class VocabQuizType {
    WORD_TO_DEFINITION,
    DEFINITION_TO_WORD,
}

data class VocabQuizQuestion(
    val wordId: Int,
    val type: VocabQuizType,
    val prompt: String,
    val correctAnswer: String,
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

        val isCorrect = answer == currentQuestion.correctAnswer
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
                            else -> "서버 응답 오류(${e.code()})로 단어 암기 테스트를 시작할 수 없어요."
                        }
                        is IOException -> "서버에 연결하지 못했습니다. 네트워크 또는 서버 상태를 확인해주세요."
                        else -> "단어 암기 테스트 문제를 불러오지 못했습니다."
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
            .map { candidate -> buildQuestion(candidate, candidates) }
    }

    private fun buildQuestion(
        candidate: VocabCandidate,
        candidates: List<VocabCandidate>,
    ): VocabQuizQuestion {
        return if (Random.nextBoolean()) {
            VocabQuizQuestion(
                wordId = candidate.wordId,
                type = VocabQuizType.DEFINITION_TO_WORD,
                prompt = candidate.definition,
                correctAnswer = candidate.word,
                correctPos = candidate.pos,
                options = buildWordOptions(correct = candidate, candidates = candidates),
            )
        } else {
            VocabQuizQuestion(
                wordId = candidate.wordId,
                type = VocabQuizType.WORD_TO_DEFINITION,
                prompt = candidate.word,
                correctAnswer = candidate.definition,
                correctPos = candidate.pos,
                options = buildDefinitionOptions(correct = candidate, candidates = candidates),
            )
        }
    }

    private fun buildWordOptions(
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
            addDistinctItems(this, samePosWords, 3)
            addDistinctItems(this, otherSavedWords, 3)
            addDistinctItems(this, fallbackWords, 3)
        }.take(3)

        return (wrongOptions + correct.word)
            .distinct()
            .shuffled()
    }

    private fun buildDefinitionOptions(
        correct: VocabCandidate,
        candidates: List<VocabCandidate>,
    ): List<String> {
        val samePosDefinitions = candidates
            .filter { it.word != correct.word && it.pos == correct.pos }
            .map { it.definition }
            .distinct()
            .shuffled()

        val otherSavedDefinitions = candidates
            .filter { it.word != correct.word && it.pos != correct.pos }
            .map { it.definition }
            .distinct()
            .shuffled()

        val fallbackDefinitions = (fallbackDefinitionPool[correct.pos].orEmpty() + fallbackDefinitionPool.values.flatten())
            .filter { it != correct.definition }
            .distinct()
            .shuffled()

        val wrongOptions = buildList {
            addDistinctItems(this, samePosDefinitions, 3)
            addDistinctItems(this, otherSavedDefinitions, 3)
            addDistinctItems(this, fallbackDefinitions, 3)
        }.take(3)

        return (wrongOptions + correct.definition)
            .distinct()
            .shuffled()
    }

    private fun addDistinctItems(
        target: MutableList<String>,
        source: List<String>,
        limit: Int,
    ) {
        source.forEach { item ->
            if (target.size >= limit) return
            if (item !in target) {
                target += item
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

        val fallbackDefinitionPool = mapOf(
            "명사" to listOf(
                "글씨를 쓰거나 그림을 그릴 때 사용하는 도구.",
                "사람이 책을 읽거나 공부하는 공간.",
                "사람과 함께 사는 친숙한 동물.",
                "시간을 확인하는 데 사용하는 물건.",
            ),
            "동사" to listOf(
                "발을 번갈아 옮기며 앞으로 움직이다.",
                "소리를 내며 즐거움을 나타내다.",
                "음식을 입으로 넣어 삼키다.",
                "새로운 지식이나 기술을 익히다.",
            ),
            "형용사" to listOf(
                "소리가 거의 나지 않고 고요하다.",
                "온도가 높아 포근한 느낌이 있다.",
                "이해하거나 해내기 쉽지 않다.",
                "마음이 즐겁고 만족스럽다.",
            ),
            "부사" to listOf(
                "속도가 높게 움직이는 모양.",
                "예상하지 못한 순간에 바로.",
                "정도가 아주 큰 상태로.",
                "소리를 거의 내지 않고 조용하게.",
            ),
        )
    }
}
