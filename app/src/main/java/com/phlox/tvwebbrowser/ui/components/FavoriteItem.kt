package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun FavoriteItemRow(
    title: String,
    url: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(R.drawable.ic_not_available), contentDescription = null, modifier = Modifier.size(54.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(50.dp)) {
            Icon(painter = painterResource(R.drawable.ic_delete_grey_400_24dp), contentDescription = "Delete")
        }
    }
}

@Preview(showBackground = true) @Composable private fun PreviewFav() {
    XeraTheme { FavoriteItemRow(title = "Xera", url = "https://xera.example.com", onDelete = {}) }
}
