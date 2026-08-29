package com.phlox.tvwebbrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

/**
 * Compose for view_settings_main.xml (550L) — scrollable settings with spinners→dropdown, switches, seekbars
 * Keeps TV focus via Modifier.focusRequester in real usage; simplified here for parity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    searchEngines: List<String> = listOf("Google","Bing","Custom"),
    selectedEngine: Int = 0,
    onEngineSelected: (Int) -> Unit = {},
    homeModes: List<String> = listOf("Xera Home","Search engine","Custom","Blank"),
    selectedHomeMode: Int = 0,
    onHomeModeSelected: (Int) -> Unit = {},
    adBlockEnabled: Boolean = true,
    onAdBlockToggled: (Boolean) -> Unit = {},
    themeOptions: List<String> = listOf("System","White","Dark"),
    selectedTheme: Int = 0,
    onThemeSelected: (Int) -> Unit = {},
    onClearCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var engineExpanded by remember { mutableStateOf(false) }
    var homeExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var allowAutoplay by remember { mutableStateOf(false) }
    var debugEnabled by remember { mutableStateOf(false) }
    var cursorSpeed by remember { mutableStateOf(0.5f) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Choose default search engine", fontSize = 16.sp)
        ExposedDropdownMenuBox(expanded = engineExpanded, onExpandedChange = { engineExpanded = !engineExpanded }) {
            OutlinedTextField(value = searchEngines.getOrElse(selectedEngine){""}, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(engineExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = engineExpanded, onDismissRequest = { engineExpanded = false }) {
                searchEngines.forEachIndexed { idx, name -> DropdownMenuItem(text = { Text(name) }, onClick = { onEngineSelected(idx); engineExpanded = false }) }
            }
        }
        Text("Home page", fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp))
        ExposedDropdownMenuBox(expanded = homeExpanded, onExpandedChange = { homeExpanded = !homeExpanded }) {
            OutlinedTextField(value = homeModes.getOrElse(selectedHomeMode){""}, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(homeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = homeExpanded, onDismissRequest = { homeExpanded = false }) {
                homeModes.forEachIndexed { idx, name -> DropdownMenuItem(text = { Text(name) }, onClick = { onHomeModeSelected(idx); homeExpanded = false }) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Toggle ads blocking", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = adBlockEnabled, onCheckedChange = onAdBlockToggled)
        }
        // Adblock details card placeholder
        if (adBlockEnabled) {
            Card(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(12.dp)) { Text("Filter lists — 5 enabled (uBlock)", fontSize = 13.sp) } }
        }
        Text("Theme", fontSize = 16.sp)
        ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = !themeExpanded }) {
            OutlinedTextField(value = themeOptions.getOrElse(selectedTheme){""}, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(themeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                themeOptions.forEachIndexed { idx, name -> DropdownMenuItem(text = { Text(name) }, onClick = { onThemeSelected(idx); themeExpanded = false }) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Keep screen on", modifier = Modifier.weight(1f)); Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Allow autoplay", modifier = Modifier.weight(1f)); Switch(checked = allowAutoplay, onCheckedChange = { allowAutoplay = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Web engine debugging", modifier = Modifier.weight(1f)); Switch(checked = debugEnabled, onCheckedChange = { debugEnabled = it })
        }
        Text("Virtual cursor max speed", fontSize = 16.sp)
        Slider(value = cursorSpeed, onValueChange = { cursorSpeed = it }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onClearCache, modifier = Modifier.padding(top = 10.dp)) { Text("Clear web cache") }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 720) @Composable private fun PreviewSettingsMain() {
    XeraTheme { SettingsMainScreen() }
}
