package com.interactiveword.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://54.180.79.55:8000/"

    fun resolveStaticUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null

        val normalizedPath = path
            .replace("\\", "/")
            .removePrefix("/")

        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath
        }

        return BASE_URL.trimEnd('/') + "/" + normalizedPath
    }

    // JWT 토큰 - TokenManager에서 주입
    var authToken: String? = null
    val api: ApiService by lazy {
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

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
