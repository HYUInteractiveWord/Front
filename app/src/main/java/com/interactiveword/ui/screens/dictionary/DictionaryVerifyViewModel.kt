package com.interactiveword.ui.screens.dictionary

import android.Manifest
import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.interactiveword.R
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.ui.components.AppNotification
import com.interactiveword.ui.components.NotiType
import com.interactiveword.ui.components.XpManager
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.util.WordCardPointManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class DictionaryVerifyUiState(
    val word: String,
    val pos: String,
    val definition: String,
    val definitionEnglish: String? = null,
    val pronunciation: String? = null,
    val audioPath: String? = null,
    val isLoadingPreview: Boolean = true,
    val isRecording: Boolean = false,
    val isVerifying: Boolean = false,
    val isSaving: Boolean = false,
    val hasRecordedOnce: Boolean = false,
    val hasVerifiedOnce: Boolean = false,
    val isMatch: Boolean? = null,
    val spokenRaw: String? = null,
    val spokenCorrected: String? = null,
    val errorMessage: String? = null,
    val saveCompleted: Boolean = false,
)

class DictionaryVerifyViewModel(
    app: Application,
    private val initialWord: String,
    private val initialPos: String,
    private val initialDefinition: String,
    private val repo: WordRepository = WordRepository(),
) : AndroidViewModel(app) {

    companion object {
        private const val SAMPLE_RATE = 16000

        fun factory(
            app: Application,
            word: String,
            pos: String,
            definition: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DictionaryVerifyViewModel::class.java)) {
                    return DictionaryVerifyViewModel(
                        app = app,
                        initialWord = word,
                        initialPos = pos,
                        initialDefinition = definition,
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private val _uiState = MutableStateFlow(
        DictionaryVerifyUiState(
            word = initialWord,
            pos = initialPos,
            definition = initialDefinition,
        )
    )
    val uiState: StateFlow<DictionaryVerifyUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var recordingActive = false

    init {
        loadPreview()
    }

    private fun loadPreview() {
        viewModelScope.launch {
            try {
                val preview = repo.previewDictionaryWord(
                    word = initialWord,
                    definition = initialDefinition,
                    pos = initialPos,
                )
                val translatedDefinition = preview.definitionTranslated ?: preview.definitionEnglish
                _uiState.value = _uiState.value.copy(
                    definition = translatedDefinition ?: _uiState.value.definition,
                    definitionEnglish = null,
                    pronunciation = preview.pronunciation,
                    audioPath = preview.audioPath,
                    isLoadingPreview = false,
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    errorMessage = e.message ?: "단어 미리보기를 불러오지 못했습니다.",
                )
            }
        }
    }

    fun playPreviewAudio() {
        val path = _uiState.value.audioPath
        val url = RetrofitClient.resolveStaticUrl(path)

        if (url == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "미리보기 TTS 경로가 없습니다."
            )
            return
        }

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
                errorMessage = "미리보기 TTS 재생 실패: ${e.message}"
            )
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (_uiState.value.isVerifying || _uiState.value.isSaving) return

        recordingActive = true
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            isMatch = null,
            spokenRaw = null,
            spokenCorrected = null,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                val pcm = withContext(Dispatchers.IO) { recordMicAudio() }
                if (pcm.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        errorMessage = "Записанный звук отсутствует.",
                    )
                    return@launch
                }

                val wav = withContext(Dispatchers.IO) { writePcmToWav(pcm) }
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    hasRecordedOnce = true,
                    isVerifying = true,
                )

                val result = repo.verifyDictionaryWord(
                    file = wav,
                    targetWord = _uiState.value.word,
                )

                _uiState.value = _uiState.value.copy(
                    isVerifying = false,
                    hasVerifiedOnce = true,
                    isMatch = result.isMatch,
                    spokenRaw = result.spokenRaw,
                    spokenCorrected = result.spokenCorrected,
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isVerifying = false,
                    errorMessage = e.message ?: "발음 검증에 실패했습니다.",
                )
            }
        }
    }

    fun stopRecording() {
        recordingActive = false
    }

    fun saveToCollection() {
        if (!_uiState.value.hasVerifiedOnce || _uiState.value.isSaving) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                val newCard = repo.createWord(
                    _uiState.value.word,
                    source = "dictionary",
                    pos = _uiState.value.pos,
                    definition = initialDefinition,
                )

                // 💡 신규 추가된 단어 ID를 미확인 목록에 등록
                WordCardPointManager.addUnseenWords(getApplication(), listOf(newCard.id))

                // 알림 추가
                XpManager.emitNotification(
                    AppNotification(
                        type = NotiType.NEW_WORD,
                        message = getApplication<Application>().getString(R.string.noti_new_word_added, _uiState.value.word),
                        color = BrandGreenLight,
                        icon = Icons.Default.MenuBook
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveCompleted = true,
                )
            } catch (e: Exception) {
                val errorMessage = parseErrorMessage(e)
                val isDuplicate = (e is HttpException && e.code() == 400 && errorMessage.contains("already in your collection", ignoreCase = true))
                
                if (isDuplicate) {
                    // 중복 에러일 경우 무시하고 이미 추가된 것으로 처리
                    cleanupDuplicates(_uiState.value.word)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveCompleted = true,
                    )
                } else {
                    val localizedError = when {
                        errorMessage.contains("Word is empty", ignoreCase = true) -> getApplication<Application>().getString(R.string.error_word_empty)
                        errorMessage.contains("Word slot limit reached", ignoreCase = true) -> getApplication<Application>().getString(R.string.error_word_slot_limit)
                        errorMessage.contains("already in your collection", ignoreCase = true) -> getApplication<Application>().getString(R.string.error_word_already_exists)
                        else -> getApplication<Application>().getString(R.string.error_unknown) + ": $errorMessage"
                    }

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = localizedError,
                    )
                }
            }
        }
    }

    private fun parseErrorMessage(e: Exception): String {
        return if (e is HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    val json = JSONObject(errorBody)
                    json.optString("detail", e.message())
                } else {
                    e.message()
                }
            } catch (ex: Exception) {
                e.message()
            }
        } else {
            e.message ?: "Unknown error"
        }
    }

    private suspend fun cleanupDuplicates(targetWord: String) {
        try {
            val allWords = repo.getMyWords()
            val duplicates = allWords.filter { it.koreanWord == targetWord }
            if (duplicates.size > 1) {
                val sorted = duplicates.sortedByDescending { it.wordPoint }
                val toDelete = sorted.drop(1)
                toDelete.forEach { repo.deleteWord(it.id) }
            }
        } catch (e: Exception) {
            // 무시
        }
    }

    fun consumeSaveCompleted() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recordMicAudio(): ByteArray {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat),
            4096,
        )

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            channelConfig,
            audioFormat,
            minBuf * 4,
        )
        val output = ByteArrayOutputStream()
        val buf = ShortArray(minBuf)

        recorder.startRecording()
        try {
            while (recordingActive) {
                val read = recorder.read(buf, 0, buf.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        val s = buf[i].toInt()
                        output.write(s and 0xFF)
                        output.write((s shr 8) and 0xFF)
                    }
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
        return output.toByteArray()
    }

    private fun writePcmToWav(pcm: ByteArray): File {
        val file = File(getApplication<Application>().cacheDir, "dictionary_verify.wav")
        val byteRate = SAMPLE_RATE * 2
        FileOutputStream(file).use { out ->
            fun writeInt(v: Int) =
                ByteArray(4) { i -> ((v shr (i * 8)) and 0xFF).toByte() }.let(out::write)
            fun writeShort(v: Int) =
                ByteArray(2) { i -> ((v shr (i * 8)) and 0xFF).toByte() }.let(out::write)

            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            writeInt(pcm.size + 36)
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            writeInt(16)
            writeShort(1)
            writeShort(1)
            writeInt(SAMPLE_RATE)
            writeInt(byteRate)
            writeShort(2)
            writeShort(16)
            out.write("data".toByteArray(Charsets.US_ASCII))
            writeInt(pcm.size)
            out.write(pcm)
        }
        return file
    }

    override fun onCleared() {
        recordingActive = false
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}
