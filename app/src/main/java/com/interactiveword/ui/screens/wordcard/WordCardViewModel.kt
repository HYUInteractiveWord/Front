package com.interactiveword.ui.screens.wordcard

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.PronunciationResponse
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import org.json.JSONObject
import com.interactiveword.R
import com.interactiveword.ui.components.XpManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.Color
import com.interactiveword.ui.components.AppNotification
import com.interactiveword.ui.components.NotiType

data class WordCardUiState(
    val card: WordCard? = null,
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val isSubmittingPronunciation: Boolean = false,
    val pronunciationResult: PronunciationResponse? = null,
    val latestPronunciationHistory: Map<String, Any>? = null,
    val savedPronunciationResult: SavedPronunciationResult? = null,
    val errorMessage: String? = null,
)

data class SavedPronunciationResult(
    val score: Float,
    val xpGained: Int,
    val pronunciation: Float,
    val formant: Float,
    val pitch: Float,
    val timing: Float,
    val isIntensityGood: Boolean,
    val recordedAt: String?,
    val penaltyFactor: Float = 1.0f,
)

class WordCardViewModel(
    private val repo: WordRepository = WordRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordCardUiState())
    val uiState: StateFlow<WordCardUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingTimerJob: Job? = null

    fun loadCard(wordId: Int, context: Context? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val card = repo.getWord(wordId)
                val latestHistory = try {
                    repo.getPronunciationHistory(wordId).firstOrNull()
                } catch (_: Exception) {
                    null
                }

                _uiState.value = WordCardUiState(
                    card = card,
                    isLoading = false,
                    latestPronunciationHistory = latestHistory,
                    savedPronunciationResult = context?.let { loadLatestPronunciationResult(it, wordId) },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun refreshCard(wordId: Int) {
        viewModelScope.launch {
            try {
                val refreshedCard = repo.getWord(wordId)
                _uiState.value = _uiState.value.copy(card = refreshedCard)
            } catch (_: Exception) {}
        }
    }

    // 1. 한국어 표제어 오디오 재생
    fun playTts() {
        val path = _uiState.value.card?.ttsAudioPath
        val url = RetrofitClient.resolveStaticUrl(path)

        if (url == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "TTS 파일 경로가 없습니다.")
            return
        }

        playUrl(url, "TTS 재생 실패")
    }

    fun playDefinitionTts() {
        val path = _uiState.value.card?.defTransAudioPath
        val url = RetrofitClient.resolveStaticUrl(path) ?: return
        playUrl(url, "Definition TTS playback failed")
    }


    fun playExampleTts(path: String?) {
        val url = RetrofitClient.resolveStaticUrl(path)

        if (url == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "예문 TTS 파일 경로가 없습니다.")
            return
        }

        playUrl(url, "예문 TTS 재생 실패")
    }

    // 공통 오디오 재생 모듈
    private fun playUrl(url: String, errorPrefix: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "$errorPrefix: ${e.message}"
            )
        }
    }

    fun togglePronunciationPractice(context: Context) {
        if (_uiState.value.isRecording) {
            stopRecordingAndSubmit(context)
        } else {
            startRecording(context)
        }
    }

    private fun startRecording(context: Context) {
        val card = _uiState.value.card
        if (card == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "단어카드 정보를 찾을 수 없습니다.")
            return
        }

        try {
            val file = File.createTempFile("pronunciation_${card.id}_", ".m4a", context.cacheDir)
            recordingFile = file

            recorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    val current = _uiState.value
                    if (!current.isRecording) break
                    _uiState.value = current.copy(recordingSeconds = current.recordingSeconds + 1)
                }
            }

            _uiState.value = _uiState.value.copy(
                isRecording = true,
                recordingSeconds = 0,
                pronunciationResult = null,
                errorMessage = context.getString(R.string.pronunciation_recording_hint),
            )
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            recordingFile = null

            _uiState.value = _uiState.value.copy(
                isRecording = false,
                recordingSeconds = 0,
                errorMessage = context.getString(R.string.pronunciation_recording_start_failed, e.message ?: ""),
            )
        }
    }

    private fun stopRecordingAndSubmit(context: Context) {
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val card = _uiState.value.card
        val file = recordingFile

        try {
            recorder?.stop()
        } catch (_: Exception) {
            // 너무 짧게 녹음하면 stop에서 예외가 날 수 있음
        } finally {
            recorder?.release()
            recorder = null
        }

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            isSubmittingPronunciation = true,
            errorMessage = context.getString(R.string.pronunciation_evaluating_message),
        )

        if (card == null || file == null || !file.exists()) {
            _uiState.value = _uiState.value.copy(
                isSubmittingPronunciation = false,
                errorMessage = context.getString(R.string.pronunciation_record_file_missing),
            )
            return
        }

        viewModelScope.launch {
            try {
                val result = repo.submitPronunciation(
                    wordCardId = card.id,
                    koreanWord = card.koreanWord,
                    ttsAudioPath = card.ttsAudioPath,
                    file = file,
                )

                saveLatestPronunciationResult(context, card.id, result)

                // 💡 XP 획득 전역 이벤트 발생
                if (result.xpGained > 0) {
                    XpManager.emitXpGain(result.xpGained)
                }

                // 💡 최고 점수 경신 알림 (결과 응답의 isNewBest가 true인 경우)
                if (result.isNewBest) {
                    XpManager.emitNotification(
                        AppNotification(
                            type = NotiType.BEST_SCORE,
                            message = context.getString(R.string.noti_word_best_score_achieved, result.score.toInt()),
                            color = Color(0xFFFFC107), // Amber/Gold
                            icon = Icons.Default.EmojiEvents
                        )
                    )
                }

                val refreshedCard = repo.getWord(card.id)

                // 💡 MASTER 달성 알림 (기존 점수가 100 미만이었다가 100이 된 경우)
                if (refreshedCard.wordPoint >= 97 && (card.wordPoint < 97)) {
                    XpManager.emitNotification(
                        AppNotification(
                            type = NotiType.MASTER_ACHIEVED,
                            message = context.getString(R.string.noti_word_master_achieved, card.koreanWord),
                            color = Color(0xFFFFC107), // Amber/Gold
                            icon = Icons.Default.EmojiEvents
                        )
                    )
                }

                val latestHistory = try {
                    repo.getPronunciationHistory(card.id).firstOrNull()
                } catch (_: Exception) {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    card = refreshedCard,
                    isSubmittingPronunciation = false,
                    pronunciationResult = result,
                    latestPronunciationHistory = latestHistory,
                    savedPronunciationResult = loadLatestPronunciationResult(context, card.id),
                    errorMessage = "발음 평가 완료: ${result.score.toInt()}점 / XP +${result.xpGained}",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingPronunciation = false,
                    errorMessage = "발음 평가 실패: ${e.message}",
                )
            }
        }
    }


    private fun saveLatestPronunciationResult(
        context: Context,
        wordCardId: Int,
        result: PronunciationResponse,
    ) {
        val details = result.details
        val now = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val json = JSONObject().apply {
            put("score", result.score)
            put("xpGained", result.xpGained)
            put("pronunciation", details?.pronunciation ?: 0f)
            put("formant", details?.formant ?: 0f)
            put("pitch", details?.pitch ?: 0f)
            put("timing", details?.timing ?: 0f)
            put("isIntensityGood", details?.isIntensityGood ?: true)
            put("recordedAt", now)
            put("penaltyFactor", result.penaltyFactor)
        }

        context.getSharedPreferences("pronunciation_result_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("word_$wordCardId", json.toString())
            .apply()
    }

    private fun loadLatestPronunciationResult(
        context: Context,
        wordCardId: Int,
    ): SavedPronunciationResult? {
        val raw = context.getSharedPreferences("pronunciation_result_cache", Context.MODE_PRIVATE)
            .getString("word_$wordCardId", null)
            ?: return null

        return try {
            val json = JSONObject(raw)
            SavedPronunciationResult(
                score = json.optDouble("score", 0.0).toFloat(),
                xpGained = json.optInt("xpGained", 0),
                pronunciation = json.optDouble("pronunciation", 0.0).toFloat(),
                formant = json.optDouble("formant", 0.0).toFloat(),
                pitch = json.optDouble("pitch", 0.0).toFloat(),
                timing = json.optDouble("timing", 0.0).toFloat(),
                isIntensityGood = json.optBoolean("isIntensityGood", true),
                recordedAt = json.optString("recordedAt").takeIf { it.isNotBlank() },
                penaltyFactor = json.optDouble("penaltyFactor", 1.0).toFloat(),
            )
        } catch (_: Exception) {
            null
        }
    }


    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null

        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        super.onCleared()
    }
}