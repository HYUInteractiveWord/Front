package com.interactiveword.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.Mission
import com.interactiveword.data.model.User
import com.interactiveword.data.model.WordCard
import com.interactiveword.data.repository.MissionRepository
import com.interactiveword.data.repository.UserRepository
import com.interactiveword.data.repository.WordRepository
import com.interactiveword.ui.components.XpManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val dailyMissions: List<Mission> = emptyList(),
    val recentWords: List<WordCard> = emptyList(),
    val wordCount: Int = 0,
    val isCaptureServiceRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val wordRepo: WordRepository = WordRepository(),
    private val missionRepo: MissionRepository = MissionRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val user = userRepo.getMe()
                val words = wordRepo.getMyWords()
                val missions = missionRepo.getDailyMissions()

                _uiState.value = _uiState.value.copy(
                    user = user,
                    recentWords = words.takeLast(4).reversed(),
                    wordCount = words.size,
                    dailyMissions = missions,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun claimMission(missionId: Int) {
        viewModelScope.launch {
            try {
                val mission = missionRepo.completeMission(missionId)
                
                // 💡 XP 획득 애니메이션 발동
                XpManager.emitXpGain(mission.xpReward)
                
                // 데이터 갱신
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleCaptureService() {
        _uiState.value = _uiState.value.copy(
            isCaptureServiceRunning = !_uiState.value.isCaptureServiceRunning,
        )
    }

    fun changeLanguage(newLanguage: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepo.updateLanguage(newLanguage)
                loadData()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepo.deleteAccount()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}