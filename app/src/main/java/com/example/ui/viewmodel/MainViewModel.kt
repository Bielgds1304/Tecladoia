package com.example.ui.viewmodel

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartKeyboardApp
import com.example.api.GeminiRepository
import com.example.api.TransformAction
import com.example.data.AiEngineMode
import com.example.data.AppConfig
import com.example.data.AppThemeMode
import com.example.data.DragonIconStyle
import com.example.data.PreferencesManager
import com.example.data.model.CustomTrigger
import com.example.data.model.TextTransformHistory
import com.example.service.SmartKeyboardAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SandboxUiState(
    val inputText: String = "Oie tudo bem hj eu tava pensando em falar sobre aquele projeto de ia q a gente combino mas n sei como comecar vc pode me ajudar?",
    val originalBackupText: String? = null,
    val lastAppliedAction: TransformAction? = null,
    val lastCustomPrompt: String? = null,
    val variationIndex: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successNotice: String? = null,
    val isSimulatedKeyboardOpen: Boolean = true
)

class MainViewModel(
    private val app: SmartKeyboardApp = SmartKeyboardApp.instance
) : ViewModel() {

    private val preferencesManager: PreferencesManager = app.preferencesManager
    private val geminiRepository: GeminiRepository = app.geminiRepository
    private val triggerDao = app.database.triggerDao()
    private val historyDao = app.database.historyDao()

    val configState: StateFlow<AppConfig> = preferencesManager.configFlow

    val triggersList: StateFlow<List<CustomTrigger>> = triggerDao.getAllTriggers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<TextTransformHistory>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAccessibilityActive: StateFlow<Boolean> = SmartKeyboardAccessibilityService.isServiceActive

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _sandboxState = MutableStateFlow(SandboxUiState())
    val sandboxState: StateFlow<SandboxUiState> = _sandboxState.asStateFlow()

    val tfliteModelInfo = app.tfliteModelManager.modelInfo
    val isDownloadingModel: StateFlow<Boolean> = app.tfliteModelManager.isDownloading
    val downloadProgress: StateFlow<Float> = app.tfliteModelManager.downloadProgress

    init {
        checkBatteryOptimization()
    }

    fun checkBatteryOptimization() {
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isBatteryOptimizationIgnored.value = powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun toggleSimulatedKeyboard() {
        _sandboxState.value = _sandboxState.value.copy(
            isSimulatedKeyboardOpen = !_sandboxState.value.isSimulatedKeyboardOpen
        )
    }

    // Sandbox Text Transforms
    fun updateSandboxText(newText: String) {
        _sandboxState.value = _sandboxState.value.copy(inputText = newText, errorMessage = null)
    }

    fun applySandboxTransform(action: TransformAction, customPrompt: String? = null) {
        val currentText = _sandboxState.value.inputText
        if (currentText.isBlank()) {
            _sandboxState.value = _sandboxState.value.copy(errorMessage = "Digite um texto para testar.")
            return
        }

        _sandboxState.value = _sandboxState.value.copy(
            isLoading = true,
            errorMessage = null,
            successNotice = null,
            originalBackupText = currentText,
            lastAppliedAction = action,
            lastCustomPrompt = customPrompt,
            variationIndex = 0
        )

        viewModelScope.launch {
            val result = geminiRepository.transformText(
                inputText = currentText,
                action = action,
                customPromptInstruction = customPrompt,
                variationIndex = 0
            )

            result.fold(
                onSuccess = { transformed ->
                    _sandboxState.value = _sandboxState.value.copy(
                        inputText = transformed,
                        isLoading = false,
                        successNotice = "Melhoria aplicada com sucesso!"
                    )
                    historyDao.insertHistory(
                        TextTransformHistory(
                            originalText = currentText,
                            transformedText = transformed,
                            actionName = action.title
                        )
                    )
                },
                onFailure = { error ->
                    _sandboxState.value = _sandboxState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Falha ao transformar texto."
                    )
                }
            )
        }
    }

    fun regenerateSandboxAlternative() {
        val backup = _sandboxState.value.originalBackupText
        val action = _sandboxState.value.lastAppliedAction
        if (backup == null || action == null) {
            _sandboxState.value = _sandboxState.value.copy(errorMessage = "Nenhuma ação anterior para variar.")
            return
        }

        val nextIndex = _sandboxState.value.variationIndex + 1
        _sandboxState.value = _sandboxState.value.copy(
            isLoading = true,
            errorMessage = null,
            variationIndex = nextIndex
        )

        viewModelScope.launch {
            val result = geminiRepository.transformText(
                inputText = backup,
                action = action,
                customPromptInstruction = _sandboxState.value.lastCustomPrompt,
                variationIndex = nextIndex
            )

            result.fold(
                onSuccess = { transformed ->
                    _sandboxState.value = _sandboxState.value.copy(
                        inputText = transformed,
                        isLoading = false,
                        successNotice = "Nova alternativa #${nextIndex + 1} gerada!"
                    )
                },
                onFailure = { error ->
                    _sandboxState.value = _sandboxState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Erro ao gerar variação."
                    )
                }
            )
        }
    }

    fun undoSandboxTransform() {
        val backup = _sandboxState.value.originalBackupText
        if (backup != null) {
            _sandboxState.value = _sandboxState.value.copy(
                inputText = backup,
                originalBackupText = null,
                successNotice = "Texto original restaurado!"
            )
        }
    }

    // Triggers Management
    fun addCustomTrigger(name: String, prompt: String, iconSymbol: String = "🎯", iconKey: String = "dragon") {
        if (name.isBlank() || prompt.isBlank()) return
        viewModelScope.launch {
            triggerDao.insertTrigger(
                CustomTrigger(
                    name = name.trim(),
                    prompt = prompt.trim(),
                    iconSymbol = iconSymbol.trim().ifBlank { "🎯" },
                    iconKey = iconKey
                )
            )
        }
    }

    fun deleteCustomTrigger(trigger: CustomTrigger) {
        viewModelScope.launch {
            triggerDao.deleteTrigger(trigger)
        }
    }

    fun toggleCustomTrigger(trigger: CustomTrigger) {
        viewModelScope.launch {
            triggerDao.updateTrigger(trigger.copy(isEnabled = !trigger.isEnabled))
        }
    }

    // Appearance & Customization
    fun setTheme(theme: AppThemeMode) {
        preferencesManager.setTheme(theme)
    }

    fun setDragonStyle(style: DragonIconStyle) {
        preferencesManager.setDragonIconStyle(style)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    fun setCustomIconUri(uriStr: String?) {
        preferencesManager.setCustomIconUri(uriStr)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    fun setButtonSize(sizeDp: Int) {
        preferencesManager.setButtonSize(sizeDp)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    fun setButtonOpacity(opacity: Float) {
        preferencesManager.setButtonOpacity(opacity)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    fun setTargetLanguage(language: String) {
        preferencesManager.setTargetLanguage(language)
    }

    fun setHapticFeedback(enabled: Boolean) {
        preferencesManager.setHapticFeedback(enabled)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        preferencesManager.setOverlayEnabled(enabled)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    fun setAutoHideWhenKeyboardCloses(enabled: Boolean) {
        preferencesManager.setAutoHideWhenKeyboardCloses(enabled)
    }

    fun setAiEngineMode(mode: AiEngineMode) {
        preferencesManager.setAiEngineMode(mode)
    }

    fun downloadLocalAiModel() {
        viewModelScope.launch {
            app.tfliteModelManager.downloadTFLiteModel()
            preferencesManager.setLocalModelDownloaded(true)
        }
    }

    fun deleteLocalAiModel() {
        app.tfliteModelManager.deleteLocalModel()
        preferencesManager.setLocalModelDownloaded(false)
    }

    fun runTFLiteBenchmark() {
        viewModelScope.launch {
            app.tfliteTextProcessor.processText("Teste de inferência neural rápida", TransformAction.CORRECT)
        }
    }

    fun setDockPosition(xRatio: Float, yRatio: Float) {
        preferencesManager.setDockPosition(xRatio, yRatio)
        SmartKeyboardAccessibilityService.currentServiceInstance?.refreshOverlayConfig()
    }

    // API Key & Security
    fun saveCustomApiKey(key: String) {
        preferencesManager.saveCustomApiKey(key)
    }

    fun getCustomApiKey(): String = preferencesManager.getCustomApiKey()

    // History
    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }
}
