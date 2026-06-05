

package com.interactiveword.util

import android.content.Context
import android.content.SharedPreferences
import com.interactiveword.data.model.WordCard

object WordCardPointManager {
    private const val PREFS_NAME = "word_card_points_prefs"
    private const val KEY_PREFIX_POINTS = "last_points_"
    private const val KEY_PREFIX_LEVEL = "last_level_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPointLevel(points: Int): Int {
        return when {
            points >= 100 -> 5 // MASTER
            points >= 76 -> 4  // 숙련
            points >= 51 -> 3  // 성장
            points >= 26 -> 2  // 연습
            else -> 1          // 새 단어
        }
    }

    /**
     * 특정 단어카드의 "마지막으로 확인한 포인트"와 현재 포인트를 비교하여 증가량을 반환합니다.
     */
    fun getUnseenPointIncrease(context: Context, card: WordCard): Int {
        val prefs = getPrefs(context)
        val lastPoints = prefs.getInt("${KEY_PREFIX_POINTS}${card.id}", 0)
        val currentPoints = card.wordPoint.coerceAtLeast(card.bestScore.toInt())
        return (currentPoints - lastPoints).coerceAtLeast(0)
    }

    /**
     * 레벨업 여부를 확인합니다.
     */
    fun checkLevelUp(context: Context, card: WordCard): Int? {
        val prefs = getPrefs(context)
        val lastLevel = prefs.getInt("${KEY_PREFIX_LEVEL}${card.id}", 1)
        val currentPoints = card.wordPoint.coerceAtLeast(card.bestScore.toInt())
        val currentLevel = getPointLevel(currentPoints)

        return if (currentLevel > lastLevel) currentLevel else null
    }

    /**
     * 현재 포인트와 레벨을 "확인함"으로 저장합니다.
     */
    fun markAsSeen(context: Context, card: WordCard) {
        val currentPoints = card.wordPoint.coerceAtLeast(card.bestScore.toInt())
        val currentLevel = getPointLevel(currentPoints)
        getPrefs(context).edit()
            .putInt("${KEY_PREFIX_POINTS}${card.id}", currentPoints)
            .putInt("${KEY_PREFIX_LEVEL}${card.id}", currentLevel)
            .apply()
    }

    private const val KEY_LAST_SEEN_NEWEST_ID = "last_seen_newest_id"
    private const val KEY_UNSEEN_IDS = "unseen_word_ids"

    /**
     * 특정 단어카드가 아직 단어장에서 확인되지 않았는지 여부
     */
    fun isWordUnseen(context: Context, cardId: Int): Boolean {
        val prefs = getPrefs(context)
        val unseenIds = prefs.getStringSet(KEY_UNSEEN_IDS, emptySet()) ?: emptySet()
        return cardId.toString() in unseenIds
    }

    /**
     * 새로운 단어들이 추가되었을 때 "미확인" 목록에 추가
     */
    fun addUnseenWords(context: Context, cardIds: List<Int>) {
        val prefs = getPrefs(context)
        val currentUnseen = prefs.getStringSet(KEY_UNSEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentUnseen.addAll(cardIds.map { it.toString() })
        prefs.edit().putStringSet(KEY_UNSEEN_IDS, currentUnseen).apply()
    }

    /**
     * 단어장에서 확인된 단어들을 "미확인" 목록에서 제거
     */
    fun markWordsAsSeen(context: Context, cardIds: List<Int>) {
        val prefs = getPrefs(context)
        val currentUnseen = prefs.getStringSet(KEY_UNSEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentUnseen.removeAll(cardIds.map { it.toString() }.toSet())
        prefs.edit().putStringSet(KEY_UNSEEN_IDS, currentUnseen).apply()
    }

    fun isNewestUnseen(context: Context, cardId: Int): Boolean {
        val prefs = getPrefs(context)
        val lastSeenId = prefs.getInt(KEY_LAST_SEEN_NEWEST_ID, -1)
        return cardId > lastSeenId
    }

    fun markNewestAsSeen(context: Context, cardId: Int) {
        val prefs = getPrefs(context)
        val lastSeenId = prefs.getInt(KEY_LAST_SEEN_NEWEST_ID, -1)
        if (cardId > lastSeenId) {
            prefs.edit().putInt(KEY_LAST_SEEN_NEWEST_ID, cardId).apply()
        }
    }
}
