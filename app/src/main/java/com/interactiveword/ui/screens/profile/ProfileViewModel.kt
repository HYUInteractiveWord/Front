package com.interactiveword.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.model.Mission
import com.interactiveword.data.model.User
import com.interactiveword.data.repository.MissionRepository
import com.interactiveword.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val dailyMissions: List<Mission> = emptyList(),
    val allMissions: List<Mission> = emptyList(),
)

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val missionRepo: MissionRepository = MissionRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val daily = missionRepo.getDailyMissions()
                val all   = missionRepo.getAllMissions()
                _uiState.value = ProfileUiState(
                    dailyMissions = daily,
                    allMissions   = all,
                )
            } catch (_: Throwable) {}
        }
    }
}
