package com.interactiveword.data.api

import com.interactiveword.data.model.DictionarySearchResponse
import com.interactiveword.data.model.LoginRequest
import com.interactiveword.data.model.Mission
import com.interactiveword.data.model.PronunciationResponse
import com.interactiveword.data.model.PronunciationSubmitRequest
import com.interactiveword.data.model.RegisterRequest
import com.interactiveword.data.model.ScanProcessRequest
import com.interactiveword.data.model.ScanUploadResponse
import com.interactiveword.data.model.TokenResponse
import com.interactiveword.data.model.User
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordCreateRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.http.Query

interface ApiService {

    // ── Auth ───────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): User

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("api/auth/me")
    suspend fun getMe(): User

    // ── Dictionary ─────────────────────────────────────────────────────────
    @GET("api/dictionary/search")
    suspend fun searchDictionary(@Query("word") word: String): DictionarySearchResponse

    // ── Words ──────────────────────────────────────────────────────────────
    @GET("api/words/")
    suspend fun getMyWords(): List<WordCard>

    @POST("api/words/")
    suspend fun createWord(@Body body: WordCreateRequest): WordCard

    @GET("api/words/{id}")
    suspend fun getWord(@Path("id") id: Int): WordCard

    @DELETE("api/words/{id}")
    suspend fun deleteWord(@Path("id") id: Int)

    // ── Scan ───────────────────────────────────────────────────────────────
    @Multipart
    @POST("api/scan/upload")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part("scan_source") scanSource: RequestBody,
    ): ScanUploadResponse

    @POST("api/scan/process")
    suspend fun processScan(@Body body: ScanProcessRequest): Map<String, Any>

    // ── Pronunciation ──────────────────────────────────────────────────────
    @POST("api/pronunciation/submit")
    suspend fun submitPronunciation(@Body body: PronunciationSubmitRequest): PronunciationResponse

    @GET("api/pronunciation/{wordCardId}/history")
    suspend fun getPronunciationHistory(@Path("wordCardId") wordCardId: Int): List<Map<String, Any>>

    // ── Missions ───────────────────────────────────────────────────────────
    @GET("api/missions/")
    suspend fun getAllMissions(): List<Mission>

    @GET("api/missions/daily")
    suspend fun getDailyMissions(): List<Mission>

    @POST("api/missions/{id}/complete")
    suspend fun completeMission(@Path("id") id: Int): Mission
}
