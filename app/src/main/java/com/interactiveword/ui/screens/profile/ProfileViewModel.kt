package com.interactiveword.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.Mission
import com.interactiveword.data.model.User
import com.interactiveword.data.repository.MissionRepository
import com.interactiveword.data.repository.UserRepository
import com.interactiveword.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class ProfileUiState(
    val user: User? = null,
    val wordsCount: Int = 0,
    val dailyMissions: List<Mission> = emptyList(),
    val allMissions: List<Mission> = emptyList(),
    val profileStatusMessage: String? = null,
    val missionStatusMessage: String? = null,
)

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val missionRepo: MissionRepository = MissionRepository(),
    private val wordRepo: WordRepository = WordRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val userResult = runCatching { userRepo.getMe() }
            val wordsResult = runCatching { wordRepo.getMyWords() }
            val dailyResult = runCatching { missionRepo.getDailyMissions() }
            val allResult = runCatching { missionRepo.getAllMissions() }

            val user = userResult.getOrNull()
            val words = wordsResult.getOrDefault(emptyList())
            val daily = dailyResult.getOrDefault(emptyList())
            val all = allResult.getOrDefault(emptyList())

            val profileStatusMessage = when {
                userResult.isFailure -> toProfileMessage(userResult.exceptionOrNull())
                wordsResult.isFailure -> toWordMessage(wordsResult.exceptionOrNull())
                else -> null
            }

            val missionStatusMessage = when {
                dailyResult.isFailure || allResult.isFailure ->
                    toMissionMessage(dailyResult.exceptionOrNull() ?: allResult.exceptionOrNull())
                daily.isEmpty() -> "현재 표시할 일일 미션이 없습니다."
                else -> null
            }

            _uiState.value = ProfileUiState(
                user = user,
                wordsCount = words.size,
                dailyMissions = daily,
                allMissions = all,
                profileStatusMessage = profileStatusMessage,
                missionStatusMessage = missionStatusMessage,
            )
        }
    }

    private fun toProfileMessage(error: Throwable?): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인 정보가 만료되어 프로필을 불러오지 못했습니다. 다시 로그인해주세요."
                else -> "서버 응답 오류(${error.code()})로 프로필을 불러오지 못했습니다."
            }
            is IOException -> "서버에 연결하지 못해 프로필을 불러오지 못했습니다."
            else -> "프로필 정보를 불러오지 못했습니다."
        }
    }

    private fun toWordMessage(error: Throwable?): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인 정보가 만료되어 단어장 개수를 불러오지 못했습니다. 다시 로그인해주세요."
                else -> "서버 응답 오류(${error.code()})로 단어장 개수를 불러오지 못했습니다."
            }
            is IOException -> "서버에 연결하지 못해 단어장 개수를 불러오지 못했습니다."
            else -> "단어장 개수를 불러오지 못했습니다."
        }
    }

    private fun toMissionMessage(error: Throwable?): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인 정보가 만료되어 일일 미션을 불러오지 못했습니다. 다시 로그인해주세요."
                else -> "서버 응답 오류(${error.code()})로 일일 미션을 불러오지 못했습니다."
            }
            is IOException -> "서버에 연결하지 못해 일일 미션을 불러오지 못했습니다."
            else -> "일일 미션을 불러오지 못했습니다."
        }
    }
}
