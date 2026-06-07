package com.interactiveword.ui.screens.profile

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordQuizItemResultRequest
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random
import com.interactiveword.ui.components.XpManager

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
    val answeredResults: List<WordQuizItemResultRequest> = emptyList(),
    val isResultSubmitted: Boolean = false,
    val submitResultMessage: String? = null,
    val submitErrorMessage: String? = null,
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
    val displayDefinition: String,
    val pos: String,
    val wordAudioPath: String?,
    val defAudioPath: String?,
)

class VocabQuizViewModel(
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabQuizUiState())
    val uiState: StateFlow<VocabQuizUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentCandidatesCache: List<VocabCandidate>? = null

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
            answeredResults = state.answeredResults + WordQuizItemResultRequest(
                wordId = currentQuestion.wordId,
                isCorrect = isCorrect,
            ),
        )
    }

    fun goToNextQuestion() {
        val state = _uiState.value
        if (!state.isAnswerChecked) return

        val nextState = state.copy(
            currentIndex = state.currentIndex + 1,
            selectedAnswer = null,
            isAnswerChecked = false,
        )

        _uiState.value = nextState

        if (nextState.isFinished) {
            submitQuizResult()
        }
    }

    private fun submitQuizResult() {
        val state = _uiState.value
        if (state.isResultSubmitted || state.answeredResults.isEmpty()) return

        viewModelScope.launch {
            try {
                val response = wordRepo.submitQuizResult(
                    quizType = "meaning",
                    results = state.answeredResults,
                )

                val mission = response.mission
                val missionText = if (mission != null) {
                    " · 미션 ${mission.progress}/${mission.target}"
                } else {
                    ""
                }

                _uiState.value = _uiState.value.copy(
                    isResultSubmitted = true,
                    submitResultMessage = "획득 XP +${response.quizXpGained}$missionText",
                    submitErrorMessage = null,
                )

                // 💡 XP 획득 전역 이벤트 발생
                if (response.quizXpGained > 0) {
                    XpManager.emitXpGain(response.quizXpGained)
                }
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    submitErrorMessage = "퀴즈 결과를 서버에 반영하지 못했습니다.",
                )
            }
        }
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

    fun playTts(text: String) {
        val candidates = currentCandidatesCache ?: return

        val matchedCandidate = candidates.find {
            it.word == text || it.displayDefinition == text || it.definition == text
        } ?: return

        val audioPath = if (text == matchedCandidate.word) {
            matchedCandidate.wordAudioPath
        } else {
            matchedCandidate.defAudioPath
        }

        if (audioPath.isNullOrBlank()) return

        val url = RetrofitClient.resolveStaticUrl(audioPath) ?: return

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            // TTS 재생 실패는 퀴즈 진행을 막지 않는다.
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.value = VocabQuizUiState(isLoading = true)

            try {
                val words = wordRepo.getMyWords()
                val candidates = buildCandidates(words)

                if (candidates.size < 4) {
                    _uiState.value = VocabQuizUiState(
                        isLoading = false,
                        errorMessage = "단어 암기 테스트를 시작하려면\n최소 4개 이상의 단어가 필요해요.\n(현재 ${candidates.size}개)",
                        emptyReason = if (words.isEmpty()) {
                            VocabQuizEmptyReason.NO_WORDS
                        } else {
                            VocabQuizEmptyReason.NO_DEFINITION_DATA
                        },
                    )
                    return@launch
                }

                currentCandidatesCache = candidates

                _uiState.value = VocabQuizUiState(
                    isLoading = false,
                    questions = buildQuestions(candidates),
                )
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

    private fun buildCandidates(words: List<WordCard>): List<VocabCandidate> {
        val language = currentLanguage()

        return words.mapNotNull { card ->
            val definition = card.definition.clean()
            val displayDefinition = localizedDefinition(card, language)
            val normalizedPos = normalizePos(card.pos)

            if (definition.isBlank() || displayDefinition.isBlank() || normalizedPos == null) {
                return@mapNotNull null
            }

            VocabCandidate(
                wordId = card.id,
                word = card.koreanWord.trim(),
                definition = definition,
                displayDefinition = displayDefinition,
                pos = normalizedPos,
                wordAudioPath = card.ttsAudioPath,
                defAudioPath = card.defTransAudioPath,
            )
        }
    }

    private fun buildQuestions(
        candidates: List<VocabCandidate>,
    ): List<VocabQuizQuestion> {
        return candidates
            .shuffled()
            .take(min(VOCAB_QUIZ_LIMIT, candidates.size))
            .map { candidate ->
                buildQuestion(
                    candidate = candidate,
                    candidates = candidates,
                )
            }
    }

    private fun buildQuestion(
        candidate: VocabCandidate,
        candidates: List<VocabCandidate>,
    ): VocabQuizQuestion {
        return if (Random.nextBoolean()) {
            VocabQuizQuestion(
                wordId = candidate.wordId,
                type = VocabQuizType.DEFINITION_TO_WORD,
                prompt = candidate.displayDefinition,
                correctAnswer = candidate.word,
                correctPos = candidate.pos,
                options = buildWordOptions(
                    correct = candidate,
                    candidates = candidates,
                ),
            )
        } else {
            VocabQuizQuestion(
                wordId = candidate.wordId,
                type = VocabQuizType.WORD_TO_DEFINITION,
                prompt = candidate.word,
                correctAnswer = candidate.displayDefinition,
                correctPos = candidate.pos,
                options = buildDefinitionOptions(
                    correct = candidate,
                    candidates = candidates,
                ),
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

        val otherWords = candidates
            .filter { it.word != correct.word && it.pos != correct.pos }
            .map { it.word }
            .distinct()
            .shuffled()

        val wrongOptions = (samePosWords + otherWords).take(3)

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
            .map { it.displayDefinition }
            .distinct()
            .shuffled()

        val otherDefinitions = candidates
            .filter { it.word != correct.word && it.pos != correct.pos }
            .map { it.displayDefinition }
            .distinct()
            .shuffled()

        val wrongOptions = (samePosDefinitions + otherDefinitions).take(3)

        return (wrongOptions + correct.displayDefinition)
            .distinct()
            .shuffled()
    }

    private fun localizedDefinition(card: WordCard, language: String): String {
        return when (language) {
            "ru" -> listOf(
                card.definitionTranslated,
                card.definitionEnglish,
                card.definition,
            ).firstClean()

            "en" -> listOf(
                card.definitionEnglish,
                card.definitionTranslated,
                card.definition,
            ).firstClean()

            else -> listOf(
                card.definition,
                card.definitionTranslated,
                card.definitionEnglish,
            ).firstClean()
        }
    }

    private fun currentLanguage(): String {
        return Locale.getDefault().language.lowercase(Locale.ROOT)
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

    private fun String?.clean(): String {
        return this?.trim().orEmpty()
    }

    private fun List<String?>.firstClean(): String {
        return firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }
}