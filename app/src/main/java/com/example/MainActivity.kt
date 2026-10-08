package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.AppearanceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SecurityAuditScreen
import com.example.ui.screens.TriggersScreen
import com.example.ui.theme.SmartKeyboardTheme
import com.example.ui.viewmodel.MainViewModel

enum class NavigationTab(val label: String) {
    HOME("Início"),
    TRIGGERS("Gatilhos"),
    APPEARANCE("Aparência"),
    SECURITY("Segurança"),
    HISTORY("Histórico")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val config by viewModel.configState.collectAsState()

            SmartKeyboardTheme(themeMode = config.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkBatteryOptimization()
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        currentTab = NavigationTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navItems = listOf(
                    NavigationTab.HOME to Icons.Default.Home,
                    NavigationTab.TRIGGERS to Icons.Default.AutoAwesome,
                    NavigationTab.APPEARANCE to Icons.Default.Palette,
                    NavigationTab.SECURITY to Icons.Default.Security,
                    NavigationTab.HISTORY to Icons.Default.History
                )

                navItems.forEach { (tab, icon) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(imageVector = icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            NavigationTab.HOME -> HomeScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.TRIGGERS -> TriggersScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.APPEARANCE -> AppearanceScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.SECURITY -> SecurityAuditScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.HISTORY -> HistoryScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
