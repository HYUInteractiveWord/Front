package com.interactiveword.data.repository

import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.model.LoginRequest
import com.interactiveword.data.model.RegisterRequest
import com.interactiveword.data.model.User

class UserRepository {
    private val api = RetrofitClient.api

    suspend fun register(username: String, email: String?, password: String, preferredLanguage: String = "ko"): User =
        api.register(RegisterRequest(username, email, password, preferredLanguage))

    suspend fun login(username: String, password: String): String {
        val response = api.login(LoginRequest(username, password))
        RetrofitClient.authToken = response.accessToken
        return response.accessToken
    }

    suspend fun getMe(): User = api.getMe()

    suspend fun updateLanguage(newLanguage: String): User {
        return api.updateMe(mapOf("preferred_language" to newLanguage))
    }

    suspend fun demoLogin(username: String, email: String?, password: String, preferredLanguage: String): User {
        return api.demoLogin(RegisterRequest(username, email, password, preferredLanguage))
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
    }
}
