package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val displayName: String, val description: String) {
    DARK_MINIMAL("Minimalista Escuro", "Preto suave grafite com contraste limpo"),
    OLED_BLACK("OLED Preto Puro", "Contraste absoluto #000000 para economia de bateria"),
    LIGHT_PAPER("Papel Claro", "Estilo editorial minimalista, fundo branco e texto nítido"),
    SLATE_GRAPHITE("Ardósia & Aço", "Tons sóbrios de cinza azulado profissional"),
    WARM_SEPIA("Sépia Aconchegante", "Tons quentes sem cansaço visual")
}

enum class DragonIconStyle(val displayName: String) {
    TATTOO_ART("Dragão Tatuagem B&W"),
    VECTOR_MINIMAL("Glifo Minimalista"),
    CUSTOM_IMAGE("Imagem Personalizada")
}

enum class AiEngineMode(val displayName: String, val description: String) {
    HYBRID("Modo Híbrido (Recomendado)", "Usa Gemini quando online; usa IA Local quando offline"),
    GEMINI_CLOUD("Apenas Nuvem (Gemini 3.5)", "Sempre usa a API do Gemini via internet"),
    LOCAL_OFFLINE("100% Offline (IA Local)", "Processa tudo no dispositivo sem gastar dados ou internet")
}

data class AppConfig(
    val themeMode: AppThemeMode = AppThemeMode.DARK_MINIMAL,
    val dragonIconStyle: DragonIconStyle = DragonIconStyle.TATTOO_ART,
    val customIconUri: String? = null,
    val buttonSizeDp: Int = 48,
    val buttonOpacity: Float = 0.95f,
    val hapticFeedback: Boolean = true,
    val targetLanguage: String = "Inglês",
    val hasCustomApiKey: Boolean = false,
    val isOverlayEnabled: Boolean = true,
    val autoHideWhenKeyboardCloses: Boolean = true,
    val aiEngineMode: AiEngineMode = AiEngineMode.HYBRID,
    val isLocalModelDownloaded: Boolean = true,
    val dockPositionYRatio: Float = 0.62f,
    val dockPositionXRatio: Float = 0.05f
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("teclado_inteligente_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AppConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AppConfig {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.DARK_MINIMAL.name) ?: AppThemeMode.DARK_MINIMAL.name
        val theme = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK_MINIMAL)

        val iconStyleStr = prefs.getString("dragon_icon_style", DragonIconStyle.TATTOO_ART.name) ?: DragonIconStyle.TATTOO_ART.name
        val iconStyle = runCatching { DragonIconStyle.valueOf(iconStyleStr) }.getOrDefault(DragonIconStyle.TATTOO_ART)

        val engineModeStr = prefs.getString("ai_engine_mode", AiEngineMode.HYBRID.name) ?: AiEngineMode.HYBRID.name
        val engineMode = runCatching { AiEngineMode.valueOf(engineModeStr) }.getOrDefault(AiEngineMode.HYBRID)

        val encryptedKey = prefs.getString("custom_api_key_enc", "") ?: ""

        return AppConfig(
            themeMode = theme,
            dragonIconStyle = iconStyle,
            customIconUri = prefs.getString("custom_icon_uri", null),
            buttonSizeDp = prefs.getInt("button_size_dp", 48),
            buttonOpacity = prefs.getFloat("button_opacity", 0.95f),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            targetLanguage = prefs.getString("target_language", "Inglês") ?: "Inglês",
            hasCustomApiKey = encryptedKey.isNotEmpty(),
            isOverlayEnabled = prefs.getBoolean("is_overlay_enabled", true),
            autoHideWhenKeyboardCloses = prefs.getBoolean("auto_hide_keyboard", true),
            aiEngineMode = engineMode,
            isLocalModelDownloaded = prefs.getBoolean("local_model_downloaded", true),
            dockPositionYRatio = prefs.getFloat("dock_y_ratio", 0.62f),
            dockPositionXRatio = prefs.getFloat("dock_x_ratio", 0.05f)
        )
    }

    fun setTheme(theme: AppThemeMode) {
        prefs.edit().putString("theme_mode", theme.name).apply()
        _configFlow.value = _configFlow.value.copy(themeMode = theme)
    }

    fun setDragonIconStyle(style: DragonIconStyle) {
        prefs.edit().putString("dragon_icon_style", style.name).apply()
        _configFlow.value = _configFlow.value.copy(dragonIconStyle = style)
    }

    fun setCustomIconUri(uri: String?) {
        prefs.edit().putString("custom_icon_uri", uri).apply()
        _configFlow.value = _configFlow.value.copy(customIconUri = uri)
    }

    fun setButtonSize(sizeDp: Int) {
        prefs.edit().putInt("button_size_dp", sizeDp).apply()
        _configFlow.value = _configFlow.value.copy(buttonSizeDp = sizeDp)
    }

    fun setButtonOpacity(opacity: Float) {
        prefs.edit().putFloat("button_opacity", opacity).apply()
        _configFlow.value = _configFlow.value.copy(buttonOpacity = opacity)
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _configFlow.value = _configFlow.value.copy(hapticFeedback = enabled)
    }

    fun setTargetLanguage(language: String) {
        prefs.edit().putString("target_language", language).apply()
        _configFlow.value = _configFlow.value.copy(targetLanguage = language)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_overlay_enabled", enabled).apply()
        _configFlow.value = _configFlow.value.copy(isOverlayEnabled = enabled)
    }

    fun setAutoHideWhenKeyboardCloses(enabled: Boolean) {
        prefs.edit().putBoolean("auto_hide_keyboard", enabled).apply()
        _configFlow.value = _configFlow.value.copy(autoHideWhenKeyboardCloses = enabled)
    }

    fun setAiEngineMode(mode: AiEngineMode) {
        prefs.edit().putString("ai_engine_mode", mode.name).apply()
        _configFlow.value = _configFlow.value.copy(aiEngineMode = mode)
    }

    fun setLocalModelDownloaded(downloaded: Boolean) {
        prefs.edit().putBoolean("local_model_downloaded", downloaded).apply()
        _configFlow.value = _configFlow.value.copy(isLocalModelDownloaded = downloaded)
    }

    fun setDockPosition(xRatio: Float, yRatio: Float) {
        prefs.edit()
            .putFloat("dock_x_ratio", xRatio)
            .putFloat("dock_y_ratio", yRatio)
            .apply()
        _configFlow.value = _configFlow.value.copy(dockPositionXRatio = xRatio, dockPositionYRatio = yRatio)
    }

    fun saveCustomApiKey(plainKey: String) {
        if (plainKey.isBlank()) {
            prefs.edit().remove("custom_api_key_enc").apply()
            _configFlow.value = _configFlow.value.copy(hasCustomApiKey = false)
        } else {
            val encrypted = CryptoManager.encrypt(plainKey.trim())
            prefs.edit().putString("custom_api_key_enc", encrypted).apply()
            _configFlow.value = _configFlow.value.copy(hasCustomApiKey = true)
        }
    }

    fun getCustomApiKey(): String {
        val encrypted = prefs.getString("custom_api_key_enc", "") ?: ""
        return if (encrypted.isNotEmpty()) CryptoManager.decrypt(encrypted) else ""
    }
}
