package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ScanRepository {
    private val api = RetrofitClient.api

    /**
     * WAV 파일을 서버에 업로드 → Demucs + Whisper 실행 → whisper_text 반환
     */
    suspend fun uploadAudio(file: File, scanSource: String = "mic"): String {
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val source = scanSource.toRequestBody("text/plain".toMediaTypeOrNull())
        val result = api.uploadAudio(body, source)
        return result["whisper_text"] ?: ""
    }
}
