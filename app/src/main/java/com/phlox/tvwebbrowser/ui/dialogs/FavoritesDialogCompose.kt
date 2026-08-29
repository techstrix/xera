package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.ui.components.FavoriteItemRow
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

data class FavoriteUi(val id: Long, val title: String, val url: String)

@Composable
fun FavoritesDialogCompose(
    items: List<FavoriteUi>,
    isLoading: Boolean,
    isEditMode: Boolean,
    onToggleEdit: () -> Unit,
    onAdd: () -> Unit,
    onFavoriteClick: (FavoriteUi) -> Unit,
    onDelete: (FavoriteUi) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bookmarks", fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Add") }
                    OutlinedButton(onClick = onToggleEdit, modifier = Modifier.weight(1f)) { Text(if (isEditMode) "Done" else "Edit") }
                }
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (items.isEmpty()) {
                    Text("No bookmarks yet", fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(items, key = { it.id }) { fav ->
                            FavoriteItemRow(
                                title = fav.title,
                                url = fav.url,
                                onDelete = { onDelete(fav) },
                                modifier = Modifier.fillMaxWidth().clickable { onFavoriteClick(fav) }
                            )
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Preview(showBackground = true) @Composable private fun PreviewFavDialog() {
    XeraTheme { FavoritesDialogCompose(listOf(FavoriteUi(1,"Xera","https://example.com")), false, false, {}, {}, {}, {}, {}) }
}
