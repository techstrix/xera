package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun ShortcutDialogCompose(
    actionTitle: String,
    currentKey: String,
    onSetKey: () -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shortcut", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row { Text("Action", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)); Text(actionTitle, maxLines = 2) }
                Row { Text("Current key", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)); Text(currentKey, fontSize = 20.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onSetKey, modifier = Modifier.width(200.dp)) { Text("Set key for action", maxLines = 1) }
                    Button(onClick = onClearKey, modifier = Modifier.width(200.dp)) { Text("Clear", maxLines = 1) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Preview(showBackground = true) @Composable private fun PreviewShortcut() {
    XeraTheme { ShortcutDialogCompose("Back", "B", {}, {}, {}) }
}
