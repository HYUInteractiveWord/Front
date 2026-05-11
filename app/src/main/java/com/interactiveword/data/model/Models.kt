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
    @SerializedName("definition_english") val definitionEnglish: String? = null,
    @SerializedName("example_sentences") val exampleSentences: List<Any>?,
    @SerializedName("tts_audio_path") val ttsAudioPath: String?,
    val level: Int,
    @SerializedName("best_score") val bestScore: Float,
    @SerializedName("scan_count") val scanCount: Int,
    val source: String,
    val pronunciation: String? = null,

    @SerializedName("word_point")
    val wordPoint: Int = 0,

    @SerializedName("speaking_count")
    val speakingCount: Int = 0,

    @SerializedName("effect_level")
    val effectLevel: Int = 0,

    @SerializedName("last_practiced_at")
    val lastPracticedAt: String? = null,
)

data class Mission(
    val id: Int,
    @SerializedName("user_id") val userId: Int? = null,
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
    val pos: String? = null,
    val definition: String? = null,
)


data class WordQuizItemResultRequest(
    @SerializedName("word_id") val wordId: Int,
    @SerializedName("is_correct") val isCorrect: Boolean,
)

data class WordQuizResultRequest(
    @SerializedName("quiz_type") val quizType: String,
    val results: List<WordQuizItemResultRequest>,
)

data class WordQuizUserSummary(
    val xp: Int,
    val rank: String,
    @SerializedName("max_word_slots") val maxWordSlots: Int,
)

data class WordQuizMissionSummary(
    @SerializedName("mission_type") val missionType: String,
    val progress: Int,
    val target: Int,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("xp_reward") val xpReward: Int,
)

data class WordQuizResultResponse(
    @SerializedName("quiz_type") val quizType: String,
    val total: Int,
    val correct: Int,
    val score: Int,
    @SerializedName("perfect_bonus") val perfectBonus: Int,
    @SerializedName("quiz_xp_gained") val quizXpGained: Int,
    val user: WordQuizUserSummary?,
    val mission: WordQuizMissionSummary?,
)

data class DictionaryCandidateInfo(
    val pos: String?,
    val definition: String?,
)

data class DictionarySearchResponse(
    val word: String? = null,
    val pos: String? = null,
    val definition: String? = null,

    @SerializedName("search_query")
    val searchQuery: String? = null,

    val candidates: Map<String, DictionaryCandidateInfo> = emptyMap(),
)

data class DictionaryPreviewRequest(
    val word: String,
    val definition: String,
    val pos: String,
)

data class DictionaryPreviewResponse(
    val word: String? = null,
    val definition: String? = null,
    @SerializedName("definition_english") val definitionEnglish: String? = null,
    val pos: String? = null,
    val pronunciation: String? = null,
    @SerializedName("audio_path") val audioPath: String? = null,
)

data class DictionaryVerifyResponse(
    @SerializedName("is_match") val isMatch: Boolean,
    @SerializedName("spoken_raw") val spokenRaw: String? = null,
    @SerializedName("spoken_corrected") val spokenCorrected: String? = null,
)

data class ScanUploadResponse(
    @SerializedName("scan_source") val scanSource: String,
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("corrected_text") val correctedText: String,
    @SerializedName("llm_raw_output") val llmRawOutput: String,
    @SerializedName("extracted_words") val extractedWords: List<String>,
    val candidates: Map<String, Map<String, String>>,
)

data class YouTubeScanRequest(
    val url: String,
    @SerializedName("end_sec") val endSec: Double,
    @SerializedName("duration_sec") val durationSec: Double = 10.0,
)

data class ScanProcessRequest(
    @SerializedName("extracted_words") val extractedWords: Map<String, Map<String, String>>,
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
