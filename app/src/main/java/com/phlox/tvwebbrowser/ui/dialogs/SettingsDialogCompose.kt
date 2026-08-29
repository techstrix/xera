package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.ui.screens.SettingsMainScreen
import com.phlox.tvwebbrowser.ui.screens.SettingsVersionScreen
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun SettingsDialogCompose(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Main", "Shortcuts", "Version & updates")
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> SettingsMainScreen(modifier = Modifier.weight(1f))
            1 -> {
                // Shortcuts placeholder — ShortcutsSettingsView not yet Compose, keep View interop
                Text("Shortcuts — TV focus mapping", modifier = Modifier.padding(22.dp))
            }
            else -> SettingsVersionScreen(
                version = "2.1.6",
                buildFlavor = "generic · geckoExcluded",
                webViewVersion = "Chrome",
                onLinkClick = {},
                onSupportClick = {},
                onLicenseClick = {},
                onPrivacyClick = {},
                onUkraineClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewSettingsDialog() {
    XeraTheme { SettingsDialogCompose(onDismiss = {}) }
}
