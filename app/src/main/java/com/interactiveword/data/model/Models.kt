package com.interactiveword.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val xp: Int,
    val rank: String,
    @SerializedName("max_word_slots") val maxWordSlots: Int,
)

data class WordCard(
    val id: Int,
    @SerializedName("korean_word") val koreanWord: String,
    val pos: String?,
    val definition: String?,
    @SerializedName("example_sentences") val exampleSentences: List<Any>?,
    @SerializedName("tts_audio_path") val ttsAudioPath: String?,
    val level: Int,
    @SerializedName("best_score") val bestScore: Float,
    @SerializedName("scan_count") val scanCount: Int,
    val source: String,
    val pronunciation: String? = null,       // 로마자 발음 (로컬 보조 필드)
)

data class Mission(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("mission_type") val missionType: String,
    val parameter: String?,
    val progress: Int,
    val target: Int,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("xp_reward") val xpReward: Int,
)

// ── 요청/응답 모델 ──────────────────────────────────────────────────────────
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class TokenResponse(@SerializedName("access_token") val accessToken: String)

data class WordCreateRequest(
    @SerializedName("korean_word") val koreanWord: String,
    val source: String = "dictionary",
)

data class ScanProcessRequest(
    @SerializedName("extracted_words") val extractedWords: List<String>,
    @SerializedName("full_text") val fullText: String? = null,
    @SerializedName("scan_source") val scanSource: String = "mic",
)

data class PronunciationSubmitRequest(
    @SerializedName("word_card_id") val wordCardId: Int,
    val score: Float,
    @SerializedName("user_pitch_data") val userPitchData: List<Float>,
    @SerializedName("reference_pitch_data") val referencePitchData: List<Float>,
    @SerializedName("dtw_distance") val dtwDistance: Float?,
)

data class PronunciationResponse(
    @SerializedName("record_id") val recordId: Int,
    val score: Float,
    @SerializedName("is_new_best") val isNewBest: Boolean,
    @SerializedName("xp_gained") val xpGained: Int,
    @SerializedName("word_card_level") val wordCardLevel: Int,
)
