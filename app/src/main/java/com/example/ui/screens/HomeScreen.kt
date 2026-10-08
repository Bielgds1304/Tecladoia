package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.TransformAction
import com.example.data.AiEngineMode
import com.example.ui.components.DragonIconView
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configState.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val sandboxState by viewModel.sandboxState.collectAsState()
    val activeTriggers by viewModel.triggersList.collectAsState()

    val scrollState = rememberScrollState()
    var isRibbonExpandedInSandbox by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Teclado Inteligente",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "IA Horizontal • Sem cara de IA • Funciona Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DragonIconView(
                iconStyle = config.dragonIconStyle,
                customUri = config.customIconUri,
                size = 42.dp
            )
        }

        // Engine & Status Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS & MOTOR DE IA",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Engine Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (config.aiEngineMode) {
                                AiEngineMode.HYBRID -> "Híbrido (Nuvem + Local)"
                                AiEngineMode.LOCAL_OFFLINE -> "100% Offline (Local)"
                                AiEngineMode.GEMINI_CLOUD -> "Gemini Nuvem"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 10.sp
                        )
                    }
                }

                // Accessibility Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityActive) Color(0xFF4CAF50) else Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Acessibilidade",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAccessibilityActive) "Ativa (Lê e substitui o texto diretamente)" else "Desativada (Necessária para funcionar no teclado)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isAccessibilityActive) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("enable_accessibility_button")
                        ) {
                            Text("Ativar", fontSize = 12.sp)
                        }
                    }
                }

                // Auto-Hide Keyboard Mode Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (config.autoHideWhenKeyboardCloses) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Auto-Ocultar sem Teclado",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (config.autoHideWhenKeyboardCloses) "Aparece só ao abrir o teclado e some ao fechar" else "Permanece sempre visível",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.autoHideWhenKeyboardCloses,
                        onCheckedChange = { viewModel.setAutoHideWhenKeyboardCloses(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Live Interactive Testing Sandbox with Horizontal Dock
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIMULADOR DE ESCRITA & DOCA HORIZONTAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Toggle simulated keyboard open/close
                    OutlinedButton(
                        onClick = {
                            if (sandboxState.isSimulatedKeyboardOpen) {
                                isRibbonExpandedInSandbox = false
                            }
                            viewModel.toggleSimulatedKeyboard()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (sandboxState.isSimulatedKeyboardOpen) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (sandboxState.isSimulatedKeyboardOpen) "Fechar Teclado" else "Abrir Teclado", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "O botão de dragão fica à esquerda e expande as opções da esquerda para a direita. A primeira opção na barra é para retrair e ao fechar o teclado ele recolhe automaticamente:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Editable Input Area
                OutlinedTextField(
                    value = sandboxState.inputText,
                    onValueChange = { viewModel.updateSandboxText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .testTag("sandbox_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    placeholder = { Text("Digite algo aqui...") }
                )

                // Live Floating Button & Horizontal Dock simulation (Left-to-Right)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0F0F))
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (sandboxState.isSimulatedKeyboardOpen) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Dragon Button Toggle on the LEFT
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { isRibbonExpandedInSandbox = !isRibbonExpandedInSandbox },
                                contentAlignment = Alignment.Center
                            ) {
                                DragonIconView(
                                    iconStyle = config.dragonIconStyle,
                                    customUri = config.customIconUri,
                                    size = 46.dp
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Horizontal Ribbon (Expands from LEFT to RIGHT!)
                            AnimatedVisibility(
                                visible = isRibbonExpandedInSandbox,
                                enter = fadeIn() + slideInHorizontally { -it },
                                exit = fadeOut() + slideOutHorizontally { -it }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .border(1.dp, Color(0xFF383838), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // 1. PRIMEIRA OPÇÃO: Retrair Doca (◀)
                                    SandboxIconButton("◀", "Retrair Barra") {
                                        isRibbonExpandedInSandbox = false
                                    }

                                    // 2. Sem Cara de IA (🌿)
                                    SandboxIconButton("🌿", "Sem Cara de IA") {
                                        viewModel.applySandboxTransform(TransformAction.NATURAL_HUMAN)
                                    }

                                    // 3. Corrigir (✍️)
                                    SandboxIconButton("✍️", "Corrigir") {
                                        viewModel.applySandboxTransform(TransformAction.CORRECT)
                                    }

                                    // 4. Expandir (⚡)
                                    SandboxIconButton("⚡", "Expandir") {
                                        viewModel.applySandboxTransform(TransformAction.EXPAND)
                                    }

                                    // 5. Traduzir (🌐)
                                    SandboxIconButton("🌐", "Traduzir (${config.targetLanguage})") {
                                        viewModel.applySandboxTransform(TransformAction.TRANSLATE)
                                    }

                                    // 6. Encurtar (✂️)
                                    SandboxIconButton("✂️", "Encurtar") {
                                        viewModel.applySandboxTransform(TransformAction.SHORTEN)
                                    }

                                    // Custom Trigger Icons (Emojis / Letters)
                                    activeTriggers.forEach { trigger ->
                                        val symbol = trigger.iconSymbol.ifBlank { trigger.name.take(1).uppercase() }
                                        SandboxIconButton(symbol, trigger.name) {
                                            viewModel.applySandboxTransform(TransformAction.CUSTOM, trigger.prompt)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Keyboard closed state: automatically retracted and hidden
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Teclado fechado: o botão retraiu e está oculto automaticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Undo and Regenerate Row
                if (sandboxState.originalBackupText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.undoSandboxTransform() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C2C2E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_undo")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Desfazer", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.regenerateSandboxAlternative() },
                            enabled = !sandboxState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_alternative")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Outra Versão (Diferente)", fontSize = 11.sp)
                        }
                    }
                }

                // Loading / Message State
                if (sandboxState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (config.aiEngineMode == AiEngineMode.LOCAL_OFFLINE) "TensorFlow Lite processando off-line..." else "IA processando texto...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                sandboxState.errorMessage?.let { error ->
                    Text(text = error, color = Color(0xFFFF5252), style = MaterialTheme.typography.bodySmall)
                }

                sandboxState.successNotice?.let { notice ->
                    Text(text = notice, color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Draggable Position Controller
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "POSIÇÃO FLUTUANTE SOBRE O TECLADO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ajuste onde o botão de dragão surge quando o teclado abre. Você também pode arrastá-lo em tempo real com o dedo em qualquer aplicativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Altura vertical na tela:", style = MaterialTheme.typography.bodySmall)
                    Text("${(config.dockPositionYRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.dockPositionYRatio,
                    onValueChange = { newY -> viewModel.setDockPosition(config.dockPositionXRatio, newY) },
                    valueRange = 0.2f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lado horizontal:", style = MaterialTheme.typography.bodySmall)
                    Text("${(config.dockPositionXRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.dockPositionXRatio,
                    onValueChange = { newX -> viewModel.setDockPosition(newX, config.dockPositionYRatio) },
                    valueRange = 0.05f..0.92f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SandboxIconButton(
    iconSymbol: String,
    tooltip: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF262628))
            .border(1.dp, Color(0xFF3E3E42), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconSymbol,
            fontSize = 17.sp
        )
    }
}
