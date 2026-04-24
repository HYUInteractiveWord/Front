package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.ScanProcessRequest
import com.interactiveword.data.model.ScanUploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ScanRepository {
    private val api = RetrofitClient.api

    suspend fun uploadAudio(file: File, scanSource: String = "mic"): ScanUploadResponse {
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val source = scanSource.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadAudio(body, source)
    }

    suspend fun processScan(
        candidates: Map<String, Map<String, String>>,
        scanSource: String = "mic",
    ) = api.processScan(ScanProcessRequest(extractedWords = candidates, scanSource = scanSource))
}
