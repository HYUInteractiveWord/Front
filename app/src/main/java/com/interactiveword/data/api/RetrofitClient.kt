package com.interactiveword.data.api

import android.content.Context
import com.interactiveword.data.local.SettingsManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var _baseUrl: String = SettingsManager.DEFAULT_BASE_URL
    val baseUrl: String get() = _baseUrl

    fun init(context: Context) {
        _baseUrl = SettingsManager.getBaseUrl(context)
        recreateApi()
    }

    fun updateBaseUrl(context: Context, newUrl: String) {
        val sanitizedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        SettingsManager.saveBaseUrl(context, sanitizedUrl)
        _baseUrl = sanitizedUrl
        recreateApi()
    }

    private fun recreateApi() {
        _api = buildApi()
    }

    fun resolveStaticUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null

        val normalizedPath = path
            .replace("\\", "/")
            .removePrefix("/")

        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath
        }

        return _baseUrl.trimEnd('/') + "/" + normalizedPath
    }

    // JWT 토큰 - TokenManager에서 주입
    var authToken: String? = null

    private var _api: ApiService? = null
    val api: ApiService
        get() {
            if (_api == null) {
                _api = buildApi()
            }
            return _api!!
        }

    private fun buildApi(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    authToken?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
