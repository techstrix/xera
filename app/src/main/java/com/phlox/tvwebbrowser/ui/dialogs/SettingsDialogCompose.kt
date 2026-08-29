package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.activity.main.dialogs.settings.ShortcutsSettingsView
import com.phlox.tvwebbrowser.ui.screens.SettingsMainScreen
import com.phlox.tvwebbrowser.ui.screens.SettingsVersionScreen
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun SettingsDialogCompose(
    settingsModel: SettingsModel? = null,
    onDismiss: () -> Unit = {},
    onVersionLink: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Main", "Shortcuts", "Version & updates")
    val context = LocalContext.current
    // Hoisted state backed by SettingsModel (preserve persistence contract) — defaults for preview when model is null
    var keepScreenOn by remember { mutableStateOf(settingsModel?.keepScreenOn?.value ?: false) }
    var allowAutoplay by remember { mutableStateOf(settingsModel?.config?.allowAutoplayMedia ?: false) }
    var debugEnabled by remember { mutableStateOf(settingsModel?.config?.webEngineDebug ?: false) }
    var cursorSpeed by remember { mutableStateOf((settingsModel?.config?.cursorMaxSpeedPercent ?: 100) / 100f) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> SettingsMainScreen(
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChanged = {
                    keepScreenOn = it
                    settingsModel?.keepScreenOn?.value = it
                },
                allowAutoplay = allowAutoplay,
                onAllowAutoplayChanged = {
                    allowAutoplay = it
                    settingsModel?.config?.allowAutoplayMedia = it
                },
                debugEnabled = debugEnabled,
                onDebugEnabledChanged = {
                    debugEnabled = it
                    settingsModel?.config?.webEngineDebug = it
                },
                cursorSpeed = cursorSpeed,
                onCursorSpeedChanged = {
                    cursorSpeed = it
                    settingsModel?.config?.cursorMaxSpeedPercent = (it * 100).toInt().coerceIn(25, 200)
                },
                modifier = Modifier.weight(1f)
            )
            1 -> {
                AndroidView(
                    factory = { ctx -> ShortcutsSettingsView(ctx) },
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
            }
            else -> SettingsVersionScreen(
                version = "2.1.6",
                buildFlavor = "generic · geckoExcluded",
                webViewVersion = "Chrome",
                onLinkClick = { onVersionLink("https://github.com/techstrix/xera") },
                onSupportClick = { onVersionLink("https://donatello.to/truefedex") },
                onLicenseClick = { onVersionLink("https://raw.githubusercontent.com/truefedex/tv-bro/master/LICENSE.md") },
                onPrivacyClick = { onVersionLink("https://raw.githubusercontent.com/truefedex/tv-bro/master/PRIVACY.md") },
                onUkraineClick = { onVersionLink("https://tv-bro-3546c.web.app/msg001.html") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewSettingsDialog() {
    XeraTheme { SettingsDialogCompose(onDismiss = {}) }
}
