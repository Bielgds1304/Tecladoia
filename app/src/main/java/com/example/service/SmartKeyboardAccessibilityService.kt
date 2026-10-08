package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.SmartKeyboardApp
import com.example.api.GeminiRepository
import com.example.api.TransformAction
import com.example.data.DragonIconStyle
import com.example.data.PreferencesManager
import com.example.data.model.CustomTrigger
import com.example.data.model.TextTransformHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmartKeyboardAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var geminiRepository: GeminiRepository

    private var lastOriginalText: String? = null
    private var lastTransformedText: String? = null
    private var lastUsedAction: TransformAction? = null
    private var lastCustomPrompt: String? = null
    private var currentVariationIndex: Int = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        var currentServiceInstance: SmartKeyboardAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentServiceInstance = this
        _isServiceActive.value = true

        val app = applicationContext as SmartKeyboardApp
        preferencesManager = app.preferencesManager
        geminiRepository = app.geminiRepository

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initFloatingOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (preferencesManager.configFlow.value.autoHideWhenKeyboardCloses) {
            checkKeyboardVisibility(event)
        }
    }

    /**
     * Checks if the virtual keyboard (IME) or an active editable input field is open.
     * Automatically hides the floating dragon button when the keyboard is closed.
     */
    private fun checkKeyboardVisibility(event: AccessibilityEvent? = null) {
        if (!preferencesManager.configFlow.value.isOverlayEnabled) {
            overlayView?.visibility = View.GONE
            return
        }

        var isKeyboardOpen = false
        try {
            val windowList = windows
            for (w in windowList) {
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    isKeyboardOpen = true
                    break
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        if (!isKeyboardOpen && event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            try {
                val source = event.source
                if (source != null && (source.isEditable || source.isFocusable)) {
                    isKeyboardOpen = true
                }
            } catch (ignored: Exception) {
            }
        }

        mainHandler.post {
            if (!isKeyboardOpen) {
                // Auto-retract ribbon back into the original dragon button when keyboard closes
                overlayView?.findViewWithTag<View>("horizontal_ribbon")?.visibility = View.GONE
            }
            overlayView?.visibility = if (isKeyboardOpen) View.VISIBLE else View.GONE
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        currentServiceInstance = null
        _isServiceActive.value = false
        removeFloatingOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingOverlay() {
        if (!preferencesManager.configFlow.value.isOverlayEnabled) return

        removeFloatingOverlay()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialX = (screenWidth * preferencesManager.configFlow.value.dockPositionXRatio).toInt()
        val initialY = (screenHeight * preferencesManager.configFlow.value.dockPositionYRatio).toInt()

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val rootLayout = FrameLayout(this)
        overlayView = rootLayout

        buildHorizontalFloatingUi(rootLayout)

        try {
            windowManager?.addView(rootLayout, windowLayoutParams)
            if (preferencesManager.configFlow.value.autoHideWhenKeyboardCloses) {
                checkKeyboardVisibility()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildHorizontalFloatingUi(rootLayout: FrameLayout) {
        rootLayout.removeAllViews()

        // Horizontal container: Dragon button on the LEFT, Ribbon on the RIGHT
        val horizontalContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Horizontal Icon Ribbon (expanded from left to right)
        val horizontalRibbon = createHorizontalIconRibbon()
        horizontalRibbon.visibility = View.GONE

        // Dragon Floating Button on the left
        val dragonButton = createDragonButton(horizontalRibbon)

        // Add Dragon Button FIRST (LEFT), then Horizontal Ribbon (RIGHT)
        horizontalContainer.addView(dragonButton)
        horizontalContainer.addView(horizontalRibbon)
        rootLayout.addView(horizontalContainer)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createDragonButton(ribbonView: View): FrameLayout {
        val sizeDp = preferencesManager.configFlow.value.buttonSizeDp
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

        val buttonContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
            background = getDragonBackgroundDrawable()
            elevation = 12f
            alpha = preferencesManager.configFlow.value.buttonOpacity
        }

        val dragonIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                val pad = (sizePx * 0.18f).toInt()
                setPadding(pad, pad, pad, pad)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            loadDragonIconInto(this)
        }

        val loadingSpinner = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (sizePx * 0.7f).toInt(),
                (sizePx * 0.7f).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
            visibility = View.GONE
            tag = "loading_spinner"
        }

        buttonContainer.addView(dragonIcon)
        buttonContainer.addView(loadingSpinner)

        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false

        buttonContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowLayoutParams?.x ?: 0
                    initialY = windowLayoutParams?.y ?: 0
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchStartX).toInt()
                    val deltaY = (event.rawY - touchStartY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                        windowLayoutParams?.x = initialX + deltaX
                        windowLayoutParams?.y = initialY + deltaY
                        try {
                            windowManager?.updateViewLayout(overlayView, windowLayoutParams)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        val displayMetrics = resources.displayMetrics
                        val curX = (windowLayoutParams?.x ?: 0).toFloat()
                        val curY = (windowLayoutParams?.y ?: 0).toFloat()
                        val ratioX = (curX / displayMetrics.widthPixels).coerceIn(0.05f, 0.92f)
                        val ratioY = (curY / displayMetrics.heightPixels).coerceIn(0.1f, 0.9f)
                        preferencesManager.setDockPosition(ratioX, ratioY)
                    } else {
                        vibrateClick()
                        toggleHorizontalRibbon(ribbonView)
                    }
                    true
                }
                else -> false
            }
        }

        return buttonContainer
    }

    private fun loadDragonIconInto(imageView: ImageView) {
        val config = preferencesManager.configFlow.value
        when (config.dragonIconStyle) {
            DragonIconStyle.TATTOO_ART -> {
                imageView.setImageResource(R.drawable.ic_dragon_tattoo)
            }
            DragonIconStyle.VECTOR_MINIMAL -> {
                imageView.setImageResource(R.drawable.ic_dragon_vector)
            }
            DragonIconStyle.CUSTOM_IMAGE -> {
                val uriStr = config.customIconUri
                if (!uriStr.isNullOrEmpty()) {
                    try {
                        imageView.setImageURI(Uri.parse(uriStr))
                    } catch (e: Exception) {
                        imageView.setImageResource(R.drawable.ic_dragon_tattoo)
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_dragon_tattoo)
                }
            }
        }
    }

    private fun getDragonBackgroundDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#121212"))
            setStroke(3, Color.parseColor("#333333"))
        }
    }

    private fun createHorizontalIconRibbon(): HorizontalScrollView {
        val scrollView = HorizontalScrollView(this).apply {
            tag = "horizontal_ribbon"
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161616"))
                cornerRadius = 24f * resources.displayMetrics.density
                setStroke(2, Color.parseColor("#2C2C2C"))
            }
            elevation = 16f
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
        }

        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            tag = "ribbon_row_layout"
        }

        scrollView.addView(rowLayout)
        return scrollView
    }

    private fun toggleHorizontalRibbon(ribbonView: View) {
        if (ribbonView.visibility == View.VISIBLE) {
            ribbonView.visibility = View.GONE
        } else {
            populateRibbonIcons(ribbonView)
            ribbonView.visibility = View.VISIBLE
        }
    }

    private fun populateRibbonIcons(ribbonView: View) {
        val rowLayout = ribbonView.findViewWithTag<LinearLayout>("ribbon_row_layout") ?: return
        rowLayout.removeAllViews()

        // 1. PRIMEIRA OPÇÃO: Retrair / Fechar Doca (◀)
        addIconAction(rowLayout, "◀", "Retrair Barra", isAccent = true) {
            ribbonView.visibility = View.GONE
        }

        // 2. Sem Cara de IA (🌿)
        addIconAction(rowLayout, "🌿", "Sem Cara de IA") {
            executeTransformOnActiveField(TransformAction.NATURAL_HUMAN)
        }

        // 3. Corrigir Ortografia (✍️)
        addIconAction(rowLayout, "✍️", "Corrigir Ortografia") {
            executeTransformOnActiveField(TransformAction.CORRECT)
        }

        // 4. Expandir Texto (⚡)
        addIconAction(rowLayout, "⚡", "Expandir Texto") {
            executeTransformOnActiveField(TransformAction.EXPAND)
        }

        // 5. Traduzir (🌐)
        addIconAction(rowLayout, "🌐", "Traduzir (${preferencesManager.configFlow.value.targetLanguage})") {
            executeTransformOnActiveField(TransformAction.TRANSLATE)
        }

        // 6. Encurtar / Direto (✂️)
        addIconAction(rowLayout, "✂️", "Encurtar") {
            executeTransformOnActiveField(TransformAction.SHORTEN)
        }

        // Gatilhos Personalizados com seus Emojis / Letras
        serviceScope.launch {
            val triggers = SmartKeyboardApp.instance.database.triggerDao().getActiveTriggersSync()
            for (trigger in triggers) {
                val symbol = trigger.iconSymbol.ifBlank { trigger.name.take(1).uppercase() }
                addIconAction(rowLayout, symbol, trigger.name) {
                    executeTransformOnActiveField(TransformAction.CUSTOM, trigger.prompt)
                }
            }

            // Desfazer se disponível (↩️)
            if (!lastOriginalText.isNullOrEmpty()) {
                addIconAction(rowLayout, "↩️", "Desfazer Alteração", isAccent = true) {
                    undoLastTransform()
                }

                addIconAction(rowLayout, "🔄", "Outra Versão (Diferente)") {
                    regenerateAlternative()
                }
            }
        }
    }

    private fun addIconAction(
        container: LinearLayout,
        iconSymbol: String,
        label: String,
        isAccent: Boolean = false,
        onClick: () -> Unit
    ) {
        val sizePx = (38 * resources.displayMetrics.density).toInt()
        val marginPx = (4 * resources.displayMetrics.density).toInt()

        val button = TextView(this).apply {
            text = iconSymbol
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isAccent) Color.parseColor("#3A3A3C") else Color.parseColor("#222222"))
                setStroke(1, Color.parseColor("#383838"))
            }
            isClickable = true
            isFocusable = true

            setOnClickListener {
                vibrateClick()
                onClick()
                overlayView?.findViewWithTag<View>("horizontal_ribbon")?.visibility = View.GONE
            }

            setOnLongClickListener {
                showToast(label)
                true
            }
        }

        container.addView(button)
    }

    /**
     * Finds active editable node without requiring text selection.
     */
    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && (focused.isEditable || focused.isFocusable)) {
            return focused
        }
        return searchEditableNode(root)
    }

    private fun searchEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.isEditable || node.className?.contains("EditText") == true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = searchEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    fun executeTransformOnActiveField(
        action: TransformAction,
        customPrompt: String? = null
    ) {
        val targetNode = findFocusedInputNode()
        if (targetNode == null) {
            showToast("Toque no campo de escrita onde deseja aplicar.")
            return
        }

        val currentText = targetNode.text?.toString() ?: ""
        if (currentText.isBlank()) {
            showToast("Campo vazio. Digite algo primeiro.")
            return
        }

        showLoading(true)

        lastOriginalText = currentText
        lastUsedAction = action
        lastCustomPrompt = customPrompt
        currentVariationIndex = 0

        serviceScope.launch {
            val result = geminiRepository.transformText(
                inputText = currentText,
                action = action,
                customPromptInstruction = customPrompt,
                variationIndex = currentVariationIndex
            )

            showLoading(false)

            result.fold(
                onSuccess = { transformed ->
                    lastTransformedText = transformed
                    val applied = applyTextToNode(targetNode, transformed)
                    if (applied) {
                        vibrateSuccess()
                        showToast("Texto melhorado com sucesso!")
                        SmartKeyboardApp.instance.database.historyDao().insertHistory(
                            TextTransformHistory(
                                originalText = currentText,
                                transformedText = transformed,
                                actionName = action.title
                            )
                        )
                    } else {
                        copyToClipboard(transformed)
                        showToast("Copiado! Pressione Colar.")
                    }
                },
                onFailure = { error ->
                    vibrateError()
                    showToast("Erro: ${error.localizedMessage ?: "Tente novamente"}")
                }
            )
        }
    }

    private fun regenerateAlternative() {
        val original = lastOriginalText
        val action = lastUsedAction
        if (original.isNullOrEmpty() || action == null) {
            showToast("Nenhuma ação para variar.")
            return
        }

        val targetNode = findFocusedInputNode()
        if (targetNode == null) {
            showToast("Campo de texto não encontrado.")
            return
        }

        currentVariationIndex++
        showLoading(true)

        serviceScope.launch {
            val result = geminiRepository.transformText(
                inputText = original,
                action = action,
                customPromptInstruction = lastCustomPrompt,
                variationIndex = currentVariationIndex
            )

            showLoading(false)

            result.fold(
                onSuccess = { transformed ->
                    lastTransformedText = transformed
                    val applied = applyTextToNode(targetNode, transformed)
                    if (applied) {
                        vibrateSuccess()
                        showToast("Variação #${currentVariationIndex + 1} aplicada!")
                    } else {
                        copyToClipboard(transformed)
                        showToast("Variação #${currentVariationIndex + 1} copiada!")
                    }
                },
                onFailure = { error ->
                    vibrateError()
                    showToast("Erro: ${error.localizedMessage}")
                }
            )
        }
    }

    private fun undoLastTransform() {
        val original = lastOriginalText
        if (original.isNullOrEmpty()) {
            showToast("Nada para desfazer.")
            return
        }

        val targetNode = findFocusedInputNode()
        if (targetNode == null) {
            showToast("Campo de texto não encontrado.")
            return
        }

        val applied = applyTextToNode(targetNode, original)
        if (applied) {
            vibrateSuccess()
            showToast("Texto original restaurado!")
            lastOriginalText = null
        } else {
            copyToClipboard(original)
            showToast("Original copiado!")
        }
    }

    private fun applyTextToNode(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (e: Exception) {
            false
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Teclado Inteligente IA", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showLoading(isLoading: Boolean) {
        mainHandler.post {
            overlayView?.findViewWithTag<ProgressBar>("loading_spinner")?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateClick() {
        if (!preferencesManager.configFlow.value.hapticFeedback) return
        performVibration(25)
    }

    private fun vibrateSuccess() {
        if (!preferencesManager.configFlow.value.hapticFeedback) return
        performVibration(50)
    }

    private fun vibrateError() {
        if (!preferencesManager.configFlow.value.hapticFeedback) return
        performVibration(80)
    }

    private fun performVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun refreshOverlayConfig() {
        mainHandler.post {
            initFloatingOverlay()
        }
    }

    private fun removeFloatingOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // View might not be attached
            }
            overlayView = null
        }
    }
}
