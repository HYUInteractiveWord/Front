package com.interactiveword.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.local.TokenDataStore
import com.interactiveword.data.local.LanguageManager
import com.interactiveword.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Loading : LoginUiState()
    object LoggedOut : LoginUiState()
    data class LoggedIn(val languageChanged: Boolean = false) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenDataStore = TokenDataStore(app)
    private val userRepo = UserRepository()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = tokenDataStore.tokenFlow.first()
            if (!token.isNullOrBlank()) {
                RetrofitClient.authToken = token
                try {
                    val languageChanged = syncLanguageFromCurrentUser()
                    _uiState.value = LoginUiState.LoggedIn(languageChanged)
                } catch (_: Exception) {
                    tokenDataStore.clearToken()
                    RetrofitClient.authToken = null
                    _uiState.value = LoginUiState.LoggedOut
                }
            } else {
                tokenDataStore.clearToken()
                RetrofitClient.authToken = null
                _uiState.value = LoginUiState.LoggedOut
            }
        }
    }


    private suspend fun syncLanguageFromCurrentUser(): Boolean {
        val context = getApplication<Application>()
        val me = userRepo.getMe()
        val serverLanguage = when {
            me.preferredLanguage.startsWith("ru", ignoreCase = true) -> "ru"
            else -> "ko"
        }

        val currentLanguage = LanguageManager.getSavedLanguage(context)
        return if (currentLanguage != serverLanguage) {
            LanguageManager.saveLanguage(context, serverLanguage)
            true
        } else {
            false
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val token = userRepo.login(username, password)
                tokenDataStore.saveToken(token)
                val languageChanged = syncLanguageFromCurrentUser()
                _uiState.value = LoginUiState.LoggedIn(languageChanged)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "로그인 실패")
            }
        }
    }

    fun register(username: String, email: String, password: String, preferredLanguage: String = "ko") {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                userRepo.register(username, email, password, preferredLanguage)
                login(username, password)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "회원가입 실패")
            }
        }
    }
}
