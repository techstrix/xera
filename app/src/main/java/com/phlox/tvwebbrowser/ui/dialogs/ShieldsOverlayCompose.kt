package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

/**
 * Compose for dialog_adblock_overlay.xml — per-host shields overlay
 * Uses default TV Bro dialog style via AlertDialog, metrics + Switch
 */
@Composable
fun ShieldsOverlayCompose(
    isEnabled: Boolean,
    blockedTab: Int,
    blockedTotal: Long,
    listsEnabled: Int,
    lastUpdate: String,
    host: String,
    onToggle: (Boolean) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shields", fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Toggle ads blocking", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Switch(checked = isEnabled, onCheckedChange = onToggle)
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(if (isEnabled) "SHIELDS ON — Xera" else "SHIELDS OFF", fontSize = 13.sp, color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Text("Blocked on this page: $blockedTab", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Text("Total blocked: $blockedTotal", fontSize = 13.sp)
                Text("Lists: $listsEnabled enabled", fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Text("Last update: $lastUpdate", fontSize = 12.sp)
                Text("Host: $host", fontSize = 12.sp)
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Manage", fontSize = 14.sp)
                TextButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) { Text("Filter lists") }
                Text("Tap outside to dismiss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f), modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Preview(showBackground = true) @Composable private fun PreviewShields() {
    XeraTheme { ShieldsOverlayCompose(true, 12, 342, 5, "never", "example.com", {}, {}, {}) }
}
