package com.interactiveword.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 개발 중: 로컬 PC 서버 주소 (에뮬레이터는 10.0.2.2, 실기기는 PC IP)
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // JWT 토큰 - TokenManager에서 주입
    var authToken: String? = null

    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
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
