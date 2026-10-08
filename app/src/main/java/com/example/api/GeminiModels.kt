package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = false)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = false)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = false)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = false)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: Any? = null
)

@JsonClass(generateAdapter = false)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)
