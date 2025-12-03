package com.aireminder.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Models
data class KeyResponse(val key: String)
data class GroqRequest(val model: String, val messages: List<Message>)
data class Message(val role: String, val content: String)
data class GroqResponse(val choices: List<Choice>)
data class Choice(val message: Message)

// API Interfaces
interface KeyApi {
    @GET("/key")
    suspend fun getGroqKey(): KeyResponse
}

interface GroqApi {
    @POST("v1/chat/completions")
    suspend fun summarize(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): GroqResponse
}

// Singleton Builder
object NetworkModule {
    fun getKeyApi(baseUrl: String): KeyApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeyApi::class.java)
    }

    fun getGroqApi(): GroqApi {
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }
}
