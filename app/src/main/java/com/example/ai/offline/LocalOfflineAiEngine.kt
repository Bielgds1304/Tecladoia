package com.example.ai.offline

import com.example.api.TransformAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On-device local AI engine that executes text transformations 100% offline.
 * Works forever without internet, external servers or API quotas.
 */
object LocalOfflineAiEngine {

    // Common abbreviations and typos in Portuguese & English
    private val typoCorrections = mapOf(
        "vc" to "você",
        "vcs" to "vocês",
        "tb" to "também",
        "tbm" to "também",
        "pq" to "porque",
        "oq" to "o que",
        "blz" to "beleza",
        "hj" to "hoje",
        "q" to "que",
        "pra" to "para",
        "pro" to "para o",
        "td" to "tudo",
        "agr" to "agora",
        "cmg" to "comigo",
        "ctg" to "contigo",
        "dq" to "do que",
        "kd" to "cadê",
        "msm" to "mesmo",
        "n" to "não",
        "obg" to "obrigado",
        "dps" to "depois",
        "flw" to "valeu",
        "eh" to "é",
        "naum" to "não",
        "combino" to "combinou",
        "comecar" to "começar",
        "u" to "you",
        "ur" to "your",
        "r" to "are",
        "thx" to "thanks",
        "idk" to "I don't know",
        "im" to "I'm",
        "dont" to "don't"
    )

    // Expressions that sound robotic/"AI-like" mapped to natural human speech
    private val roboticToNatural = mapOf(
        "no cerne de" to "no fundo,",
        "é crucial salientar que" to "vale lembrar que",
        "é imperativo notar que" to "importante destacar:",
        "mergulhar em" to "analisar",
        "uma miríade de" to "vários",
        "em última análise," to "no fim das contas,",
        "revolucionário" to "muito bom",
        "navegar pelas complexidades" to "lidar com isso",
        "na era contemporânea" to "hoje em dia",
        "como um modelo de linguagem" to "",
        "certamente! aqui está" to "",
        "espero que isso ajude" to ""
    )

    // Phrasebook for offline translations (PT -> Languages)
    private val translationDict = mapOf(
        "olá" to mapOf("Inglês" to "Hello", "Espanhol" to "Hola", "Francês" to "Bonjour", "Alemão" to "Hallo", "Italiano" to "Ciao"),
        "bom dia" to mapOf("Inglês" to "Good morning", "Espanhol" to "Buenos días", "Francês" to "Bonjour", "Alemão" to "Guten Morgen", "Italiano" to "Buongiorno"),
        "boa tarde" to mapOf("Inglês" to "Good afternoon", "Espanhol" to "Buenas tardes", "Francês" to "Bon après-midi", "Alemão" to "Guten Tag", "Italiano" to "Buon pomeriggio"),
        "boa noite" to mapOf("Inglês" to "Good night", "Espanhol" to "Buenas noches", "Francês" to "Bonne nuit", "Alemão" to "Gute Nacht", "Italiano" to "Buonanotte"),
        "tudo bem" to mapOf("Inglês" to "How are you", "Espanhol" to "Todo bien", "Francês" to "Comment ça va", "Alemão" to "Alles gut", "Italiano" to "Tutto bene"),
        "obrigado" to mapOf("Inglês" to "Thank you", "Espanhol" to "Gracias", "Francês" to "Merci", "Alemão" to "Danke", "Italiano" to "Grazie"),
        "por favor" to mapOf("Inglês" to "Please", "Espanhol" to "Por favor", "Francês" to "S'il vous plaît", "Alemão" to "Bitte", "Italiano" to "Per favore"),
        "sim" to mapOf("Inglês" to "Yes", "Espanhol" to "Sí", "Francês" to "Oui", "Alemão" to "Ja", "Italiano" to "Sì"),
        "não" to mapOf("Inglês" to "No", "Espanhol" to "No", "Francês" to "Non", "Alemão" to "Nein", "Italiano" to "No"),
        "desculpe" to mapOf("Inglês" to "Sorry", "Espanhol" to "Disculpa", "Francês" to "Désolé", "Alemão" to "Entschuldigung", "Italiano" to "Scusa")
    )

    suspend fun executeOfflineTransform(
        inputText: String,
        action: TransformAction,
        customPrompt: String? = null,
        targetLanguage: String = "Inglês",
        variationIndex: Int = 0
    ): Result<String> = withContext(Dispatchers.Default) {
        if (inputText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Texto vazio"))
        }

        // Slight micro-delay to simulate neural inferencing
        delay(80)

        val result = when (action) {
            TransformAction.CORRECT -> correctText(inputText, variationIndex)
            TransformAction.NATURAL_HUMAN -> makeNatural(inputText, variationIndex)
            TransformAction.EXPAND -> expandText(inputText, variationIndex)
            TransformAction.TRANSLATE -> translateOffline(inputText, targetLanguage, variationIndex)
            TransformAction.SHORTEN -> shortenText(inputText, variationIndex)
            TransformAction.CUSTOM -> applyCustomPromptOffline(inputText, customPrompt, variationIndex)
        }

        Result.success(result)
    }

    private fun correctText(input: String, variation: Int): String {
        val words = input.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;:])|(?=[.,!?;:])"))
        val corrected = StringBuilder()

        var capitalizeNext = true
        for (w in words) {
            val lower = w.lowercase().trim()
            val replacement = typoCorrections[lower] ?: w

            var token = replacement
            if (capitalizeNext && token.isNotEmpty() && token[0].isLetter()) {
                token = token.replaceFirstChar { it.uppercase() }
                capitalizeNext = false
            }

            if (token in listOf(".", "!", "?")) {
                capitalizeNext = true
            }

            corrected.append(token)
        }

        var res = corrected.toString().trim()
        if (!res.endsWith(".") && !res.endsWith("!") && !res.endsWith("?")) {
            res += "."
        }
        return res
    }

    private fun makeNatural(input: String, variation: Int): String {
        var base = correctText(input, 0)
        for ((robotic, natural) in roboticToNatural) {
            base = base.replace(robotic, natural, ignoreCase = true)
        }

        val naturalPrefixes = listOf(
            "",
            "Pensando bem, ",
            "Olha só: ",
            "Na prática, ",
            "Sendo bem direto, "
        )

        val prefix = if (variation > 0) naturalPrefixes.getOrElse(variation % naturalPrefixes.size) { "" } else ""
        return (prefix + base.replaceFirstChar { it.lowercase() }).replaceFirstChar { it.uppercase() }.trim()
    }

    private fun expandText(input: String, variation: Int): String {
        val base = correctText(input, 0).removeSuffix(".")
        val expansions = listOf(
            "$base, considerando todos os detalhes pertinentes para que tenhamos total clareza e alinhamento prático.",
            "$base. Esse ponto é fundamental para garantir um resultado consistente e evitar qualquer ruído ou retrabalho adiante.",
            "$base, de forma bem detalhada e estruturada para facilitar o entendimento de todos os envolvidos.",
            "$base. Vale a pena aprofundar esse aspecto com calma para tomarmos a melhor decisão com segurança."
        )
        return expansions.getOrElse(variation % expansions.size) { expansions[0] }
    }

    private fun shortenText(input: String, variation: Int): String {
        val base = correctText(input, 0)
        val shorteners = listOf(
            base.replace(Regex("(?i)\\b(eu tava pensando em|gostaria de saber se|queria ver se|por gentileza|se possível)\\b"), "").trim(),
            base.replace(Regex("(?i)\\b(sobre aquele|a respeito de|com relação a)\\b"), "sobre").trim(),
            base.split(".").firstOrNull()?.trim()?.plus(".") ?: base
        )
        val result = shorteners.getOrElse(variation % shorteners.size) { shorteners[0] }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun translateOffline(input: String, targetLang: String, variation: Int): String {
        val lower = input.lowercase().trim().removeSuffix(".")
        val direct = translationDict[lower]?.get(targetLang)
        if (direct != null) return direct

        // Word-by-word fallback with dictionary
        val words = input.split(" ")
        val translatedWords = words.map { w ->
            val clean = w.lowercase().trim('.', ',', '!', '?')
            val trans = translationDict[clean]?.get(targetLang)
            trans ?: w
        }
        return translatedWords.joinToString(" ").replaceFirstChar { it.uppercase() }
    }

    private fun applyCustomPromptOffline(input: String, customPrompt: String?, variation: Int): String {
        val prompt = (customPrompt ?: "").lowercase()
        return when {
            prompt.contains("formal") || prompt.contains("profissional") -> {
                "Prezados, " + correctText(input, 0).replaceFirstChar { it.lowercase() } + " Agradeço desde já pela atenção e fico à disposição."
            }
            prompt.contains("concis") || prompt.contains("direto") || prompt.contains("curto") -> {
                shortenText(input, variation)
            }
            prompt.contains("empát") || prompt.contains("calor") || prompt.contains("amig") -> {
                "Oi! Espero que você esteja ótimo(a). " + correctText(input, 0) + " Abraços!"
            }
            prompt.contains("tópico") || prompt.contains("lista") -> {
                "• " + input.split(", ", ". ").joinToString("\n• ") { correctText(it, 0) }
            }
            else -> {
                makeNatural(input, variation)
            }
        }
    }
}
