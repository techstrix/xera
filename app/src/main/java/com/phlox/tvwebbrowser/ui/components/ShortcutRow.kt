package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun ShortcutRow(title: String, key: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(15.dp)) {
        Text(title, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(key, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Preview(showBackground = true) @Composable private fun PreviewShortcut() { XeraTheme { ShortcutRow("Back", "B") } }
