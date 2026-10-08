package com.example.api

import com.example.BuildConfig
import com.example.ai.offline.LocalOfflineAiEngine
import com.example.ai.offline.TFLiteTextProcessor
import com.example.data.AiEngineMode
import com.example.data.PreferencesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class TransformAction(val title: String, val subtitle: String, val iconSymbol: String, val iconName: String) {
    NATURAL_HUMAN("Sem Cara de IA", "Texto fluido, autêntico e com voz humana", "🌿", "person"),
    CORRECT("Corrigir Ortografia", "Corrige erros, acentuação e pontuação", "✍️", "spellcheck"),
    EXPAND("Expandir Texto", "Desenvolve a ideia com detalhes e profundidade", "⚡", "unfold_more"),
    TRANSLATE("Traduzir", "Traduz com fluência para o idioma selecionado", "🌐", "translate"),
    SHORTEN("Encurtar / Direto", "Sintetiza direto ao ponto sem rodeios", "✂️", "compress"),
    CUSTOM("Gatilho Personalizado", "Instrução customizada pelo usuário", "🎯", "auto_awesome")
}

class GeminiRepository(
    private val preferencesManager: PreferencesManager,
    private val tfliteTextProcessor: TFLiteTextProcessor? = null
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    private fun resolveApiKey(): String {
        val customKey = preferencesManager.getCustomApiKey().trim()
        if (customKey.isNotEmpty()) return customKey
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    private suspend fun executeTFLiteOrLocal(
        inputText: String,
        action: TransformAction,
        customPromptInstruction: String?,
        targetLanguage: String,
        variationIndex: Int
    ): Result<String> {
        return if (tfliteTextProcessor != null) {
            val result = tfliteTextProcessor.processText(
                inputText = inputText,
                action = action,
                customPrompt = customPromptInstruction,
                targetLanguage = targetLanguage,
                variationIndex = variationIndex
            )
            result.map { it.processedText }
        } else {
            LocalOfflineAiEngine.executeOfflineTransform(
                inputText = inputText,
                action = action,
                customPrompt = customPromptInstruction,
                targetLanguage = targetLanguage,
                variationIndex = variationIndex
            )
        }
    }

    suspend fun transformText(
        inputText: String,
        action: TransformAction,
        customPromptInstruction: String? = null,
        targetLanguage: String = preferencesManager.configFlow.value.targetLanguage,
        variationIndex: Int = 0
    ): Result<String> = withContext(Dispatchers.IO) {
        if (inputText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("O campo de texto está vazio."))
        }

        val engineMode = preferencesManager.configFlow.value.aiEngineMode

        // If user selected 100% Offline mode, execute purely on device with TensorFlow Lite
        if (engineMode == AiEngineMode.LOCAL_OFFLINE) {
            return@withContext executeTFLiteOrLocal(
                inputText, action, customPromptInstruction, targetLanguage, variationIndex
            )
        }

        // Try Gemini Cloud API
        val apiKey = resolveApiKey()
        val isCloudUsable = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!isCloudUsable) {
            if (engineMode == AiEngineMode.HYBRID) {
                // Seamless offline fallback with TensorFlow Lite
                return@withContext executeTFLiteOrLocal(
                    inputText, action, customPromptInstruction, targetLanguage, variationIndex
                )
            } else {
                return@withContext Result.failure(
                    IllegalStateException("Chave da API Gemini não configurada. Ative o Modo Offline com TensorFlow Lite ou insira uma chave.")
                )
            }
        }

        val variationNote = if (variationIndex > 0) {
            "\nIMPORTANTE: Esta é a tentativa de variação #$variationIndex. O usuário não gostou da versão anterior, portanto formule uma construção frasal nitidamente DIFERENTE, com novo vocabulário e outro ritmo, mas preservando o sentido original."
        } else ""

        val systemInstructionText = when (action) {
            TransformAction.CORRECT -> """
                Você é um revisor ortográfico e gramatical de alto nível.
                Sua tarefa: corrigir quaisquer falhas de pontuação, ortografia, regência ou digitação no texto do usuário.
                Regras estritas:
                - Preserve fielmente o tom, gírias intencionais e a voz do autor.
                - NUNCA adicione saudações, introduções, aspas ou explicações.
                - Retorne EXCLUSIVAMENTE o texto final corrigido.$variationNote
            """.trimIndent()

            TransformAction.NATURAL_HUMAN -> """
                Você é um especialista em escrita humana autêntica e natural.
                Sua tarefa: transformar o texto do usuário para que soe 100% natural, fluido e sem qualquer "cara de IA" (sem clichês robóticos, sem chavões artificiais como 'mergulhar em', 'jornada', 'crucial', 'no cerne', sem construções mecânicas).
                Regras estritas:
                - Mantenha a intenção original exata do usuário.
                - Use linguagem viva, humana, orgânica e envolvente.
                - NUNCA adicione saudações, aspas ou explicações adicionais.
                - Retorne EXCLUSIVAMENTE o texto melhorado.$variationNote
            """.trimIndent()

            TransformAction.EXPAND -> """
                Você é um redator habilidoso.
                Sua tarefa: expandir e enriquecer o texto do usuário, desenvolvendo os pensamentos, adicionando argumentos ou contexto claro e elegante, sem enrolação.
                Regras estritas:
                - Retorne EXCLUSIVAMENTE o texto expandido.
                - Não adicione introduções ou comentários metacognitivos.$variationNote
            """.trimIndent()

            TransformAction.TRANSLATE -> """
                Você é um tradutor nativo profissional.
                Sua tarefa: traduzir o texto do usuário com extrema naturalidade idiomática para o idioma: $targetLanguage.
                Regras estritas:
                - Adapte expressões idiomáticas mantendo o tom exato.
                - Retorne EXCLUSIVAMENTE a tradução final.$variationNote
            """.trimIndent()

            TransformAction.SHORTEN -> """
                Você é um editor conciso.
                Sua tarefa: sintetizar o texto do usuário da forma mais direta, assertiva e enxuta possível, cortando excessos sem perder nenhuma informação essencial.
                Regras estritas:
                - Retorne EXCLUSIVAMENTE o texto resumido.$variationNote
            """.trimIndent()

            TransformAction.CUSTOM -> """
                Você é um assistente de escrita com IA executando a instrução personalizada abaixo:
                ${customPromptInstruction ?: "Melhore o texto com clareza."}
                Regras estritas:
                - Retorne EXCLUSIVAMENTE o texto resultante da instrução aplicada ao conteúdo fornecido.
                - Sem introduções, aspas ou explicações.$variationNote
            """.trimIndent()
        }

        val temperature = if (action == TransformAction.CORRECT) {
            0.2f
        } else if (variationIndex > 0) {
            0.95f
        } else {
            0.65f
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = inputText))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstructionText))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                topP = 0.95f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val generatedText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (!generatedText.isNullOrEmpty()) {
                val cleanedText = cleanOutput(generatedText)
                Result.success(cleanedText)
            } else {
                if (engineMode == AiEngineMode.HYBRID) {
                    executeTFLiteOrLocal(inputText, action, customPromptInstruction, targetLanguage, variationIndex)
                } else {
                    Result.failure(Exception("Nenhum texto retornado pela IA."))
                }
            }
        } catch (e: Exception) {
            if (engineMode == AiEngineMode.HYBRID) {
                // Fallback to TensorFlow Lite on-device AI if network fails
                executeTFLiteOrLocal(inputText, action, customPromptInstruction, targetLanguage, variationIndex)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun cleanOutput(text: String): String {
        var result = text.trim()
        if (result.startsWith("\"") && result.endsWith("\"") && result.length > 2) {
            result = result.substring(1, result.length - 1).trim()
        }
        if (result.startsWith("```") && result.endsWith("```")) {
            val lines = result.lines()
            if (lines.size >= 2) {
                result = lines.subList(1, lines.size - 1).joinToString("\n").trim()
            }
        }
        return result
    }
}
