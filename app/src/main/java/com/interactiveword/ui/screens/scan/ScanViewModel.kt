package com.interactiveword.ui.screens.scan

import android.Manifest
import android.app.Application
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.repository.ScanRepository
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.service.AudioCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.interactiveword.ui.components.AppNotification
import com.interactiveword.ui.components.NotiType
import com.interactiveword.ui.components.XpManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import com.interactiveword.R
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.util.WordCardPointManager
import retrofit2.HttpException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class ScanType { MIC, MEDIA, YOUTUBE }

data class ScanWordResult(
    val word: String,
    val pos: String?,
    val definition: String?,
)

data class ScanUiState(
    val isRecording: Boolean = false,
    val scanType: ScanType = ScanType.MIC,
    val elapsedSeconds: Int = 0,
    val detectedWords: List<ScanWordResult> = emptyList(),
    val addedWords: Set<String> = emptySet(),
    val loadingWords: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val context = getApplication<Application>()

    private val scanRepo = ScanRepository()
    private val wordRepo = WordRepository()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    @Volatile private var recordingActive = false

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val MAX_REC_SECONDS = 30
    }

    // ── [1] 마이크 스캔 ─────────────────────────────────────────────────────

    fun startMicRecording() {
        recordingActive = true
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            scanType = ScanType.MIC,
            elapsedSeconds = 0,
            detectedWords = emptyList(),
            addedWords = emptySet(),
            error = null,
        )

        viewModelScope.launch {
            val timerJob = launch {
                var sec = 0
                while (isActive) {
                    delay(1000)
                    if (!_uiState.value.isRecording) break
                    sec++
                    _uiState.value = _uiState.value.copy(elapsedSeconds = sec)
                    if (sec >= MAX_REC_SECONDS) { recordingActive = false; break }
                }
            }
            try {
                val pcm = withContext(Dispatchers.IO) { recordMicAudio() }
                timerJob.cancel()
                if (pcm.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(isRecording = false, isLoading = true)
                    val wav = withContext(Dispatchers.IO) { writePcmToWav(pcm) }
                    uploadAndProcess(wav, "mic")
                } else {
                    _uiState.value = _uiState.value.copy(isRecording = false)
                }
            } catch (e: Exception) {
                timerJob.cancel()
                _uiState.value = _uiState.value.copy(
                    isRecording = false, isLoading = false,
                    error = "녹음 실패: ${e.message}",
                )
            }
        }
    }

    fun stopRecording() { recordingActive = false }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recordMicAudio(): ByteArray {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat  = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = maxOf(AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat), 4096)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelConfig, audioFormat, minBuf * 4,
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

    // ── [2] 미디어 캡처 ─────────────────────────────────────────────────────

    fun startCaptureService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    fun startDirectCapture(uri: Uri, startMs: Long, endMs: Long) {
        val durationMs = endMs - startMs
        _uiState.value = _uiState.value.copy(
            scanType      = ScanType.MEDIA,
            detectedWords = emptyList(),
            addedWords    = emptySet(),
            error         = null,
            isLoading     = true,
        )
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val pcm = withContext(Dispatchers.IO) {
                    extractAndResampleAudio(ctx, uri, durationMs, startMs)
                }
                val wav = withContext(Dispatchers.IO) { writePcmToWav(pcm) }
                uploadAndProcess(wav, "media")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "추출 실패: ${e.message}",
                )
            }
        }
    }

    private fun extractAndResampleAudio(
        ctx: android.content.Context,
        uri: Uri,
        durationMs: Long,
        startMs: Long = 0L,
    ): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(ctx, uri, null)

        var trackIdx = -1
        var srcFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIdx = i; srcFormat = fmt; break
            }
        }
        require(trackIdx >= 0) { "오디오 트랙을 찾을 수 없습니다" }

        val srcRate     = srcFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val srcChannels = srcFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime        = srcFormat.getString(MediaFormat.KEY_MIME)!!
        val endUs       = (startMs + durationMs) * 1000L

        extractor.selectTrack(trackIdx)
        if (startMs > 0L) {
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(srcFormat, null, null, 0)
        codec.start()

        val rawOut   = ByteArrayOutputStream()
        val bufInfo  = MediaCodec.BufferInfo()
        var inputDone   = false
        var outputDone  = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf  = codec.getInputBuffer(idx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0 || extractor.sampleTime > endUs) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outIdx = codec.dequeueOutputBuffer(bufInfo, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outIdx >= 0) {
                        val buf   = codec.getOutputBuffer(outIdx)!!
                        val bytes = ByteArray(bufInfo.size)
                        buf.get(bytes)
                        rawOut.write(bytes)
                        codec.releaseOutputBuffer(outIdx, false)
                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            codec.stop(); codec.release()
            extractor.release()
        }

        return resampleToMono16k(rawOut.toByteArray(), srcRate, srcChannels)
    }

    private fun resampleToMono16k(pcm: ByteArray, srcRate: Int, srcChannels: Int): ByteArray {
        val srcSamples = pcm.size / (2 * srcChannels)
        val dstSamples = (srcSamples.toLong() * SAMPLE_RATE / srcRate).toInt()
        val out = ByteArrayOutputStream(dstSamples * 2)

        for (i in 0 until dstSamples) {
            val srcIdx = (i.toLong() * srcRate / SAMPLE_RATE).toInt().coerceAtMost(srcSamples - 1)
            var sum = 0L
            for (ch in 0 until srcChannels) {
                val b = (srcIdx * srcChannels + ch) * 2
                val lo = pcm[b].toInt() and 0xFF
                val hi = pcm[b + 1].toInt() and 0xFF
                sum += (lo or (hi shl 8)).toShort().toInt()
            }
            val mono = (sum / srcChannels).toInt().coerceIn(-32768, 32767)
            out.write(mono and 0xFF)
            out.write((mono shr 8) and 0xFF)
        }
        return out.toByteArray()
    }

    // ── [3] YouTube 공유 스캔 ─────────────────────────────────────────────────

    fun startYoutubeScan(url: String) {
        _uiState.value = _uiState.value.copy(
            scanType      = ScanType.YOUTUBE,
            detectedWords = emptyList(),
            addedWords    = emptySet(),
            error         = null,
            isLoading     = true,
        )

        viewModelScope.launch {
            try {
                // WebView(Chrome 엔진)로 자막 fetch → YouTube 브라우저 세션 필요 문제 우회
                val transcriptText = com.interactiveword.util.YouTubeWebViewFetcher(getApplication()).fetchTranscript(url)
                val response = scanRepo.scanYouTube(transcriptText)
                val words = response.candidates.map { (word, info) ->
                    ScanWordResult(word = word, pos = info["pos"], definition = info["definition"])
                }
                _uiState.value = _uiState.value.copy(isLoading = false, detectedWords = words)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = "분석 실패: ${e.message}",
                )
            }
        }
    }

    // ── 공통 ────────────────────────────────────────────────────────────────

    private fun writePcmToWav(pcm: ByteArray): File {
        val file     = File(getApplication<Application>().cacheDir, "scan_${System.currentTimeMillis()}.wav")
        val byteRate = SAMPLE_RATE * 2
        FileOutputStream(file).use { out ->
            fun wi(v: Int) = ByteArray(4) { i -> ((v shr (i * 8)) and 0xFF).toByte() }.let(out::write)
            fun ws(v: Int) = ByteArray(2) { i -> ((v shr (i * 8)) and 0xFF).toByte() }.let(out::write)
            out.write("RIFF".toByteArray(Charsets.US_ASCII)); wi(pcm.size + 36)
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII)); wi(16)
            ws(1); ws(1); wi(SAMPLE_RATE); wi(byteRate); ws(2); ws(16)
            out.write("data".toByteArray(Charsets.US_ASCII)); wi(pcm.size)
            out.write(pcm)
        }
        return file
    }

    private suspend fun uploadAndProcess(file: File, source: String) {
        try {
            val response = scanRepo.uploadAudio(file, source)
            val words = response.candidates.map { (word, info) ->
                ScanWordResult(word = word, pos = info["pos"], definition = info["definition"])
            }
            _uiState.value = _uiState.value.copy(isLoading = false, detectedWords = words)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false, error = "분석 실패: ${e.message}",
            )
        }
    }

    fun addWordToCollection(word: String, pos: String?, definition: String?) {
        val state = _uiState.value
        if (word in state.addedWords || word in state.loadingWords) return

        _uiState.value = state.copy(loadingWords = state.loadingWords + word)

        viewModelScope.launch {
            try {
                val newCard = wordRepo.createWord(
                    word = word,
                    source = "dictionary",
                    pos = pos,
                    definition = definition
                )
                
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

                // 중복 단어 정리 (포인트 높은 것 유지)
                cleanupDuplicates(word)

                _uiState.value = _uiState.value.copy(
                    addedWords = _uiState.value.addedWords + word,
                    loadingWords = _uiState.value.loadingWords - word
                )
            } catch (e: Exception) {
                val errorMessage = parseErrorMessage(e)
                val isDuplicate = (e is HttpException && e.code() == 400 && errorMessage.contains("already in your collection", ignoreCase = true))
                
                if (isDuplicate) {
                    // 중복 에러일 경우 무시하고 이미 추가된 것으로 처리
                    cleanupDuplicates(word)
                    _uiState.value = _uiState.value.copy(
                        addedWords = _uiState.value.addedWords + word,
                        loadingWords = _uiState.value.loadingWords - word
                    )
                } else {
                    val localizedError = when {
                        errorMessage.contains("Word is empty", ignoreCase = true) -> context.getString(R.string.error_word_empty)
                        errorMessage.contains("Word slot limit reached", ignoreCase = true) -> context.getString(R.string.error_word_slot_limit)
                        errorMessage.contains("already in your collection", ignoreCase = true) -> context.getString(R.string.error_word_already_exists)
                        else -> context.getString(R.string.error_unknown) + ": $errorMessage"
                    }

                    _uiState.value = _uiState.value.copy(
                        error = localizedError,
                        loadingWords = _uiState.value.loadingWords - word
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
            val allWords = wordRepo.getMyWords()
            val duplicates = allWords.filter { it.koreanWord == targetWord }
            if (duplicates.size > 1) {
                // 포인트 높은 순으로 정렬
                val sorted = duplicates.sortedByDescending { it.wordPoint }
                val toKeep = sorted.first()
                val toDelete = sorted.drop(1)
                
                toDelete.forEach { 
                    wordRepo.deleteWord(it.id)
                }
            }
        } catch (e: Exception) {
            // 정리 실패 시 무시
        }
    }

    fun dismissWord(word: String) {
        _uiState.value = _uiState.value.copy(
            detectedWords = _uiState.value.detectedWords.filter { it.word != word },
        )
    }

    override fun onCleared() {
        super.onCleared()
        recordingActive = false
    }
}
