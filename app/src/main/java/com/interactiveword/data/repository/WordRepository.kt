package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.DictionaryPreviewRequest
import com.interactiveword.data.model.DictionaryPreviewResponse
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordCreateRequest
import com.interactiveword.data.model.WordQuizResultResponse
import com.interactiveword.data.model.WordQuizResultRequest
import com.interactiveword.data.model.WordQuizItemResultRequest
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

    suspend fun createWord(
        word: String,
        source: String = "dictionary",
        pos: String? = null,
        definition: String? = null,
        dryRun: Boolean = false,
    ): WordCard = api.createWord(
        WordCreateRequest(
            koreanWord = word,
            source = source,
            pos = pos,
            definition = definition,
        )
    )

    suspend fun deleteWord(id: Int) = api.deleteWord(id)

    suspend fun submitQuizResult(
        quizType: String,
        results: List<WordQuizItemResultRequest>,
    ): WordQuizResultResponse = api.submitWordQuizResult(
        WordQuizResultRequest(
            quizType = quizType,
            results = results,
        )
    )
}
