package com.interactiveword.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val xp: Int,
    val rank: String,
    @SerializedName("max_word_slots") val maxWordSlots: Int,
    @SerializedName("preferred_language") val preferredLanguage: String = "en",
)

data class WordCard(
    val id: Int,
    @SerializedName("korean_word") val koreanWord: String,
    val pos: String?,
    val definition: String?,
    @SerializedName("definition_translated") val definitionTranslated: String? = null,
    @SerializedName("definition_english") val definitionEnglish: String? = null,
    @SerializedName("example_sentences") val exampleSentences: List<Any>?,
    @SerializedName("tts_audio_path") val ttsAudioPath: String?,
    @SerializedName("def_trans_audio_path") val defTransAudioPath: String? = null,
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
data class RegisterRequest(
    val username: String,
    val email: String?,
    val password: String,
    @SerializedName("preferred_language") val preferredLanguage: String = "en",
)
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
data class CheckedWord(
    val id: Int,
    @SerializedName("korean_word") val koreanWord: String,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("word_point") val wordPoint: Int,
    @SerializedName("effect_level") val effectLevel: Int
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
    @SerializedName("checked_words") val checkedWords: List<CheckedWord>? = emptyList()
)

data class DictionaryCandidateInfo(
    val pos: String?,
    val definition: String?,
    @SerializedName("definition_translated") val definitionTranslated: String? = null,
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
    @SerializedName("definition_translated") val definitionTranslated: String? = null,
    @SerializedName("def_trans_audio_path") val defTransAudioPath: String? = null,
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
    @SerializedName("transcript_text") val transcriptText: String,
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
    @SerializedName("word_point") val wordPoint: Int = 0,
    @SerializedName("effect_level") val effectLevel: Int = 0,
    @SerializedName("penalty_factor") val penaltyFactor: Float = 1.0f,

    val graphs: Map<String, String>? = null,
    val details: PronunciationDetails? = null,
    @SerializedName("raw_graph_data") val rawGraphData: RawGraphData? = null,
)

data class PronunciationDetails(
    val pronunciation: Float,
    val formant: Float,
    val pitch: Float,
    val timing: Float,
    @SerializedName("is_intensity_good") val isIntensityGood: Boolean,
)

data class RawGraphData(
    @SerializedName("tts_time") val ttsTime: List<Float> = emptyList(),
    @SerializedName("tts_pitch") val ttsPitch: List<Float> = emptyList(),
    @SerializedName("user_time") val userTime: List<Float> = emptyList(),
    @SerializedName("user_pitch") val userPitch: List<Float> = emptyList(),
)
