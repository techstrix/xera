package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun NotificationCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_block_popups), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, fontSize = 20.sp, maxLines = 1, modifier = Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(showBackground = true) @Composable private fun PreviewNotif() {
    XeraTheme { NotificationCard("Popup blocked") }
}
