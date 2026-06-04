package com.interactiveword.ui.screens.profile

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
    val wordAudioPath: String?,
    val definitionAudioPath: String?,
)

data class PosQuizUiState(
    val isLoading: Boolean = true,
    val questions: List<PosQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerChecked: Boolean = false,
    val correctCount: Int = 0,
    val answeredResults: List<WordQuizItemResultRequest> = emptyList(),
    val isResultSubmitted: Boolean = false,
    val submitResultMessage: String? = null,
    val submitErrorMessage: String? = null,
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

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null

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
                    quizType = "pos",
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

    fun playTts(audioPath: String?) {
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

    fun speakText(context: Context, text: String) {
        if (text.isBlank()) return

        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onCleared()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.value = PosQuizUiState(isLoading = true)

            try {
                val words = wordRepo.getMyWords()
                val validQuestions = buildQuestions(words)

                if (validQuestions.size < 4) {
                    _uiState.value = PosQuizUiState(
                        isLoading = false,
                        errorMessage = "품사 퀴즈를 시작하려면\n최소 4개 이상의 단어가 필요해요.\n(현재 ${validQuestions.size}개)",
                        emptyReason = if (words.isEmpty()) {
                            PosQuizEmptyReason.NO_WORDS
                        } else {
                            PosQuizEmptyReason.NO_POS_DATA
                        },
                    )
                    return@launch
                }

                _uiState.value = PosQuizUiState(
                    isLoading = false,
                    questions = validQuestions
                        .shuffled()
                        .take(min(POS_QUIZ_LIMIT, validQuestions.size)),
                )
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
        val language = currentLanguage()

        return words.mapNotNull { card ->
            val definition = localizedDefinition(card, language)
            val normalizedPos = normalizePos(card.pos)

            if (definition.isBlank() || normalizedPos == null) {
                return@mapNotNull null
            }

            PosQuizQuestion(
                wordId = card.id,
                word = card.koreanWord,
                definition = definition,
                correctPos = normalizedPos,
                wordAudioPath = card.ttsAudioPath,
                definitionAudioPath = card.defTransAudioPath,
            )
        }
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

    private fun List<String?>.firstClean(): String {
        return firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }
}