package com.phlox.tvwebbrowser.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.model.HistoryItem
import com.phlox.tvwebbrowser.ui.components.HistoryHeader
import com.phlox.tvwebbrowser.ui.components.HistoryItem
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

/**
 * Compose for activity_history.xml (39L) — PinnedSectionListView → LazyColumn stickyHeader
 * Keeps pagination via onScroll -> loadMore, voice search, multi-select delete
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onItemLongClick: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteSelected: () -> Unit,
    isMultiSelect: Boolean,
    selectedIds: Set<Long>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // Pagination trigger
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val last = listState.layoutInfo.visibleItemsInfo.last().index
            if (last >= items.size - 2) onLoadMore()
        }
    }

    // Single source of truth for selection — keeps row rendering and delete availability in sync
    fun isSelected(item: HistoryItem): Boolean = selectedIds.contains(item.id) || item.selected
    val hasSelection = items.any { !it.isDateHeader && isSelected(it) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(items, key = { it.id }) { item ->
                if (item.isDateHeader) {
                    // Use title as date header text
                    HistoryHeader(date = item.title ?: "")
                } else {
                    val selected = isSelected(item)
                    val timeStr = if (item.time != 0L) java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.time)) else ""
                    HistoryItem(
                        time = timeStr,
                        title = item.title,
                        url = item.url,
                        isSelectionVisible = isMultiSelect,
                        isSelected = selected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onItemClick(item) },
                                onLongClick = { onItemLongClick(item) }
                            )
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                }
            }
        }
        // Clear button (top-end, matches btnClear alignParentEnd)
        Button(
            onClick = onClearHistory,
            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
        ) { Text("Clear") }
        // Delete selected (centerEnd, matches ibDelete visibility gone -> visible with animation)
        if (isMultiSelect && hasSelection) {
            FilledIconButton(
                onClick = onDeleteSelected,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            ) { Text("⌫") }
        }
    }
}

// Preview helpers
@Preview(showBackground = true, widthDp = 960, heightDp = 540)
@Composable private fun PreviewHistoryScreen() {
    XeraTheme {
        HistoryScreen(
            items = listOf(
                HistoryItem().apply { title = "Today"; isDateHeader = true; id = 1; time = System.currentTimeMillis() },
                HistoryItem().apply { title = "Xera Home"; url = "https://example.com"; time = System.currentTimeMillis(); id = 2 },
                HistoryItem().apply { title = "Reddit"; url = "https://reddit.com"; time = System.currentTimeMillis(); id = 3 }
            ),
            onItemClick = {}, onItemLongClick = {}, onClearHistory = {}, onDeleteSelected = {},
            isMultiSelect = false, selectedIds = emptySet(), onLoadMore = {}
        )
    }
}
