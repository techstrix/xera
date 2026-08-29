package com.phlox.tvwebbrowser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.ui.components.DownloadItem
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun DownloadsScreen(
    items: List<Download>,
    onItemClick: (Download) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    LaunchedEffect(state.firstVisibleItemIndex) {
        if (state.layoutInfo.visibleItemsInfo.isNotEmpty() && state.layoutInfo.visibleItemsInfo.last().index >= items.size - 2) onLoadMore()
    }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (items.isEmpty()) {
            Text(stringResource(R.string.nothing), fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items, key = { it.id }) { dl ->
                    if (dl.isDateHeader) {
                        Text(dl.filename, modifier = Modifier.padding(15.dp))
                    } else {
                        val prog = if (dl.size > 0) dl.bytesReceived.toFloat() / dl.size else null
                        DownloadItem(
                            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(dl.time)),
                            sizeText = "${dl.bytesReceived}/${dl.size}",
                            title = dl.filename,
                            url = dl.url,
                            progress = prog,
                            isCircularVisible = dl.size == 0L,
                            modifier = Modifier.padding(vertical = 4.dp).clickable { onItemClick(dl) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540)
@Composable private fun PreviewDownloads() {
    XeraTheme { DownloadsScreen(items = emptyList(), onItemClick = {}, onLoadMore = {}) }
}
