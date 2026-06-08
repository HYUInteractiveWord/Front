package com.interactiveword.ui.screens.profile

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordQuizItemResultRequest
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed
import com.interactiveword.ui.components.XpManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.min

// ==========================================
// ViewModel & State
// ==========================================

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
    val definition: String, // 타겟 언어 뜻으로 채워집니다
    val correctPos: String,
    val wordAudioPath: String?,
    val definitionAudioPath: String?,
    val options: List<String> = emptyList(),
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
        private val ALL_POS = listOf("명사", "대명사", "수사", "동사", "형용사", "관형사", "부사", "조사", "감탄사")
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
            // 재생 오류 무시
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

                // 1. 유효한 질문 필터링 (타겟 언어 뜻 우선 적용)
                val validQuestions = words.mapNotNull { card ->
                    val definition = card.definition?.trim().orEmpty()
                    val displayDef = card.definitionTranslated?.trim()
                        ?: card.definitionEnglish?.trim()
                        ?: definition

                    val normalizedPos = normalizePos(card.pos)
                    if (displayDef.isBlank() || normalizedPos == null) return@mapNotNull null

                    PosQuizQuestion(
                        wordId = card.id,
                        word = card.koreanWord,
                        definition = displayDef,
                        correctPos = normalizedPos,
                        wordAudioPath = card.ttsAudioPath,
                        definitionAudioPath = card.defTransAudioPath
                    )
                }

                // 2. 4개 미만 진입 보호 로직
                if (validQuestions.size < 4) {
                    _uiState.value = PosQuizUiState(
                        isLoading = false,
                        errorMessage = "품사 퀴즈를 시작하려면\n최소 4개 이상의 단어가 필요해요.\n(현재 ${validQuestions.size}개)",
                        emptyReason = PosQuizEmptyReason.NO_WORDS,
                    )
                    return@launch
                }

                val shuffledQuestions = validQuestions.shuffled().take(min(POS_QUIZ_LIMIT, validQuestions.size))

                // 사용자 단어장에 존재하는 모든 품사 추출
                val existingPosList = validQuestions.map { it.correctPos }.distinct()

                // 각 질문에 대해 랜덤 보기 생성
                val questionsWithOptions = shuffledQuestions.map { question ->
                    // 1. 정답 포함
                    val questionOptions = mutableSetOf(question.correctPos)

                    // 2. 단어장에 있는 다른 품사들 중 랜덤 추가 (최대 4개까지)
                    val otherExistingPos = (existingPosList - question.correctPos).shuffled()
                    for (pos in otherExistingPos) {
                        if (questionOptions.size >= 4) break
                        questionOptions.add(pos)
                    }

                    // 3. 4개가 안 되면 전체 품사 중 랜덤 추가
                    if (questionOptions.size < 4) {
                        val remainingAllPos = (ALL_POS - questionOptions).shuffled()
                        for (pos in remainingAllPos) {
                            if (questionOptions.size >= 4) break
                            questionOptions.add(pos)
                        }
                    }

                    question.copy(options = questionOptions.toList().shuffled())
                }

                _uiState.value = PosQuizUiState(
                    isLoading = false,
                    questions = questionsWithOptions,
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

    private fun normalizePos(raw: String?): String? {
        val pos = raw?.trim().orEmpty()
        if (pos.isBlank()) return null
        return when {
            "대명사" in pos -> "대명사"
            "명사" in pos -> "명사"
            "수사" in pos -> "수사"
            "동사" in pos -> "동사"
            "형용사" in pos -> "형용사"
            "관형사" in pos -> "관형사"
            "부사" in pos -> "부사"
            "조사" in pos -> "조사"
            "감탄사" in pos -> "감탄사"
            else -> null
        }
    }
}

@Composable
private fun getPosString(pos: String?): String {
    if (pos == null) return ""
    return when {
        pos.contains("대명사") -> stringResource(R.string.pos_pronoun)
        pos.contains("명사") -> stringResource(R.string.pos_noun)
        pos.contains("수사") -> stringResource(R.string.pos_numeral)
        pos.contains("동사") -> stringResource(R.string.pos_verb)
        pos.contains("형용사") -> stringResource(R.string.pos_adjective)
        pos.contains("관형사") -> stringResource(R.string.pos_determiner)
        pos.contains("부사") -> stringResource(R.string.pos_adverb)
        pos.contains("조사") -> stringResource(R.string.pos_particle)
        pos.contains("감탄사") -> stringResource(R.string.pos_interjection)
        else -> pos
    }
}
