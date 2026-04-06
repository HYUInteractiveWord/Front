package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.model.WordCreateRequest

class WordRepository {
    private val api = RetrofitClient.api

    suspend fun getMyWords(): List<WordCard> = api.getMyWords()

    suspend fun getWord(id: Int): WordCard = api.getWord(id)

    /**
     * @param dryRun true면 실제 저장 없이 사전 정보만 조회 (Dictionary 검색 미리보기용)
     * 현재 백엔드는 dryRun을 지원하지 않으므로 실제로는 저장됨 - 추후 별도 엔드포인트 추가 필요
     */
    suspend fun createWord(word: String, source: String = "dictionary", dryRun: Boolean = false): WordCard =
        api.createWord(WordCreateRequest(koreanWord = word, source = source))

    suspend fun deleteWord(id: Int) = api.deleteWord(id)
}
