package com.interactiveword.ui.screens.profile

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordQuizItemResultRequest
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.ui.components.XpManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.min
import kotlin.random.Random

private const val EXAMPLE_QUIZ_LIMIT = 5

enum class ExampleQuizType {
    KOREAN_TO_TRANSLATION, // 문제: 한국어 예문 -> 선택지: 번역 예문
    TRANSLATION_TO_KOREAN, // 문제: 번역 예문 -> 선택지: 한국어 예문
}

data class ExampleQuizQuestion(
    val wordId: Int,
    val type: ExampleQuizType,
    val prompt: String,
    val correctAnswer: String,
    val options: List<String>,
    val promptAudioPath: String? = null,
    val optionAudioPaths: Map<String, String?> = emptyMap()
)

data class ExampleQuizUiState(
    val isLoading: Boolean = true,
    val questions: List<ExampleQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerChecked: Boolean = false,
    val correctCount: Int = 0,
    val answeredResults: List<WordQuizItemResultRequest> = emptyList(),
    val isResultSubmitted: Boolean = false,
    val submitResultMessage: String? = null,
    val submitErrorMessage: String? = null,
    val errorMessage: String? = null,
) {
    val totalQuestions: Int get() = questions.size
    val isFinished: Boolean get() = totalQuestions > 0 && currentIndex >= totalQuestions
    val currentQuestion: ExampleQuizQuestion?
        get() = if (currentIndex in questions.indices) questions[currentIndex] else null
}

class ExampleQuizViewModel(
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExampleQuizUiState())
    val uiState: StateFlow<ExampleQuizUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

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
                // 백엔드에는 우선 'meaning' 타입으로 보냄 (혹은 필요시 'example' 추가)
                val response = wordRepo.submitQuizResult(
                    quizType = "example", 
                    results = state.answeredResults,
                )

                _uiState.value = _uiState.value.copy(
                    isResultSubmitted = true,
                    submitResultMessage = "획득 XP +${response.quizXpGained}",
                    submitErrorMessage = null,
                )

                if (response.quizXpGained > 0) {
                    XpManager.emitXpGain(response.quizXpGained)
                }
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    submitErrorMessage = "결과를 서버에 반영하지 못했습니다.",
                )
            }
        }
    }

    fun restartQuiz() { loadQuestions() }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.value = ExampleQuizUiState(isLoading = true)
            try {
                val words = wordRepo.getMyWords()
                
                // 예문이 있는 단어들만 필터링
                val validWords = words.filter { !it.exampleSentences.isNullOrEmpty() }
                
                if (validWords.isEmpty()) {
                    _uiState.value = ExampleQuizUiState(
                        isLoading = false,
                        errorMessage = "학습 예문이 포함된 단어가 없습니다.\n단어를 먼저 추가해 보세요!"
                    )
                    return@launch
                }

                val questions = validWords.shuffled().take(EXAMPLE_QUIZ_LIMIT).mapNotNull { card ->
                    buildQuestionForCard(card, validWords)
                }

                if (questions.isEmpty()) {
                    _uiState.value = ExampleQuizUiState(isLoading = false, errorMessage = "문제를 생성할 수 없습니다.")
                } else {
                    _uiState.value = ExampleQuizUiState(isLoading = false, questions = questions)
                }

            } catch (e: Exception) {
                _uiState.value = ExampleQuizUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    private fun buildQuestionForCard(card: WordCard, allWords: List<WordCard>): ExampleQuizQuestion? {
        val examples = card.exampleSentences ?: return null
        if (examples.isEmpty()) return null

        // 랜덤하게 하나의 정답 예문 선택
        val correctIndex = Random.nextInt(examples.size)
        val correctExample = examples[correctIndex] as? Map<*, *> ?: return null
        
        val kr = getExValue(correctExample, "korean", "kr", "sentence") ?: return null
        val trans = getExValue(correctExample, "translation", "russian", "ru", "english", "en") ?: return null

        val isKrToTrans = Random.nextBoolean()
        val targetCorrect = if (isKrToTrans) trans else kr
        
        // 오답 리스트 생성 (먼저 같은 단어의 다른 예문들)
        val wrongOptionsFromSame = examples.filterIndexed { index, _ -> index != correctIndex }
            .mapNotNull { it as? Map<*, *> }
            .mapNotNull { getExValue(it, if (isKrToTrans) "translation" else "korean", "ru", "en", "kr", "sentence") }
            .toMutableSet()

        // 부족하면 다른 단어의 예문에서 가져옴
        if (wrongOptionsFromSame.size < 3) {
            val otherExamples = allWords.filter { it.id != card.id }
                .flatMap { it.exampleSentences ?: emptyList<Any>() }
                .mapNotNull { it as? Map<*, *> }
                .shuffled()
            
            for (ex in otherExamples) {
                val value = getExValue(ex, if (isKrToTrans) "translation" else "korean", "ru", "en", "kr", "sentence")
                if (value != null && value != targetCorrect) {
                    wrongOptionsFromSame.add(value)
                    if (wrongOptionsFromSame.size >= 3) break
                }
            }
        }

        if (wrongOptionsFromSame.size < 3) return null 

        val finalOptions = (wrongOptionsFromSame.take(3) + targetCorrect).shuffled()
        
        // 💡 모든 선택지의 오디오 경로를 매핑하여 캐시
        val audioPathMap = mutableMapOf<String, String?>()
        
        // 정답 오디오
        audioPathMap[targetCorrect] = if (isKrToTrans) {
            getExValue(correctExample, "trans_audio_path", "translation_audio_path")
        } else {
            getExValue(correctExample, "audio_path", "tts_audio_path")
        }
        
        // 오답 오디오 찾기
        for (option in wrongOptionsFromSame) {
            val matchedEx = allWords.flatMap { it.exampleSentences ?: emptyList<Any>() }
                .mapNotNull { it as? Map<*, *> }
                .find { getExValue(it, if (isKrToTrans) "translation" else "korean", "ru", "en", "kr", "sentence") == option }
            
            if (matchedEx != null) {
                audioPathMap[option] = getExValue(matchedEx, if (isKrToTrans) "trans_audio_path" else "audio_path", "translation_audio_path", "tts_audio_path")
            }
        }

        return if (isKrToTrans) {
            ExampleQuizQuestion(
                wordId = card.id,
                type = ExampleQuizType.KOREAN_TO_TRANSLATION,
                prompt = kr,
                correctAnswer = trans,
                options = finalOptions,
                promptAudioPath = getExValue(correctExample, "audio_path", "tts_audio_path"),
                optionAudioPaths = audioPathMap
            )
        } else {
            ExampleQuizQuestion(
                wordId = card.id,
                type = ExampleQuizType.TRANSLATION_TO_KOREAN,
                prompt = trans,
                correctAnswer = kr,
                options = finalOptions,
                promptAudioPath = getExValue(correctExample, "trans_audio_path", "translation_audio_path"),
                optionAudioPaths = audioPathMap
            )
        }
    }

    private fun getExValue(map: Map<*, *>, vararg keys: String): String? {
        for (key in keys) {
            val v = map[key]?.toString()
            if (!v.isNullOrBlank() && v != "null") return v
        }
        return null
    }

    fun playTts(path: String?) {
        if (path.isNullOrBlank()) return
        val url = RetrofitClient.resolveStaticUrl(path) ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        mediaPlayer?.release()
        super.onCleared()
    }
}
