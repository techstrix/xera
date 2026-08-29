package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

/**
 * Compose equivalent of view_history_header_item.xml
 * XML: LinearLayout vertical, padding 15dp, background day_night_list_header_background_color (#ccc / #3f3f3f), TextView tvDate 30sp bold
 */
@Composable
fun HistoryHeader(
    date: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(15.dp)
    ) {
        Text(
            text = date,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "HistoryHeader Light", showBackground = true)
@Composable
private fun HistoryHeaderPreviewLight() {
    XeraTheme(darkTheme = false) {
        HistoryHeader(date = "Today — 29 Aug 2026")
    }
}

@Preview(name = "HistoryHeader Dark", showBackground = true)
@Composable
private fun HistoryHeaderPreviewDark() {
    XeraTheme(darkTheme = true) {
        HistoryHeader(date = "Today — 29 Aug 2026")
    }
}
