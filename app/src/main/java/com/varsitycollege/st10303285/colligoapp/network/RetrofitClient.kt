package com.varsitycollege.st10303285.colligoapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Creates a singleton Retrofit client to be used throughout the app

object RetrofitClient {

    private const val BASE_URL = "https://api-hkjlyqfi7q-uc.a.run.app" //backend URL


    // OkHttp client with interceptor to add auth token
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(AuthTokenInterceptor()) // attaches Firebase ID token
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Retrofit instance (singleton)
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}