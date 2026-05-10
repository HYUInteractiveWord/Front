package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.DictionaryPreviewRequest
import com.interactiveword.data.model.DictionaryPreviewResponse
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordCreateRequest
import com.interactiveword.data.model.DictionarySearchResponse
import com.interactiveword.data.model.DictionaryVerifyResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class WordRepository {
    private val api = RetrofitClient.api

    suspend fun getMyWords(): List<WordCard> = api.getMyWords()

    suspend fun getWord(id: Int): WordCard = api.getWord(id)

    suspend fun searchDictionary(word: String): DictionarySearchResponse =
    api.searchDictionary(word)

    suspend fun previewDictionaryWord(
        word: String,
        definition: String,
        pos: String,
    ): DictionaryPreviewResponse = api.previewDictionary(
        DictionaryPreviewRequest(
            word = word,
            definition = definition,
            pos = pos,
        )
    )

    suspend fun verifyDictionaryWord(
        file: File,
        targetWord: String,
    ): DictionaryVerifyResponse {
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val word = targetWord.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.verifyDictionary(body, word)
    }

    /**
     * @param dryRun true면 실제 저장 없이 사전 정보만 조회 (Dictionary 검색 미리보기용)
     * 현재 백엔드는 dryRun을 지원하지 않으므로 실제로는 저장됨 - 추후 별도 엔드포인트 추가 필요
     */
    suspend fun createWord(word: String, source: String = "dictionary", dryRun: Boolean = false): WordCard =
        api.createWord(WordCreateRequest(koreanWord = word, source = source))

    suspend fun deleteWord(id: Int) = api.deleteWord(id)
}
