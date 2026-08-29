package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun DownloadItem(
    time: String,
    sizeText: String,
    title: String,
    url: String,
    progress: Float?, // 0..1 null = indeterminate
    isCircularVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sizeText, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 30.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            if (progress != null) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            } else if (isCircularVisible) {
                // placeholder for legacy progressBar2 gone state
            }
        }
        if (isCircularVisible && progress == null) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp).padding(start = 12.dp))
        }
    }
}

@Preview(showBackground = true) @Composable private fun PreviewDownload() {
    XeraTheme { DownloadItem(time = "14:32", sizeText = "12.4 MB", title = "xera.apk", url = "https://github.com/techstrix/xera/releases/xera.apk", progress = 0.6f) }
}
