package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineDialog(
    engines: List<String>,
    selectedIndex: Int,
    customUrl: String,
    onEngineSelected: (Int) -> Unit,
    onCustomUrlChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedIndex) }
    val isCustom = selected == engines.size - 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose default search engine", fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = engines.getOrElse(selected) { "" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        engines.forEachIndexed { idx, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { selected = idx; onEngineSelected(idx); expanded = false })
                        }
                    }
                }
                if (isCustom) {
                    Text("URL", fontSize = 16.sp, modifier = Modifier.padding(top = 5.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = onCustomUrlChanged,
                        placeholder = { Text("Search engine URL with query placeholder [query]") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true) @Composable private fun PreviewSearchEngine() {
    XeraTheme { SearchEngineDialog(engines = listOf("Google","Bing","Custom"), selectedIndex = 0, customUrl = "", onEngineSelected = {}, onCustomUrlChanged = {}, onConfirm = {}, onDismiss = {}) }
}
