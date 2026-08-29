package com.phlox.tvwebbrowser.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun NewFavoriteItemDialogCompose(
    initialTitle: String,
    initialUrl: String,
    onCancel: () -> Unit,
    onDone: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var titleEdit by remember { mutableStateOf(false) }
    var urlEdit by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("New bookmark", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), thickness = 1.dp, modifier = Modifier.padding(bottom = 10.dp))
                Text("Title", modifier = Modifier.padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (titleEdit) OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, modifier = Modifier.weight(1f))
                    else Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { titleEdit = !titleEdit }) { Icon(painterResource(R.drawable.ic_mode_edit_grey_400_18dp), null) }
                }
                Text("URL", modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (urlEdit) OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true, modifier = Modifier.weight(1f))
                    else Text(url, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { urlEdit = !urlEdit }) { Icon(painterResource(R.drawable.ic_mode_edit_grey_400_18dp), null) }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onDone(title, url) }, modifier = Modifier.weight(1f)) { Text("Done") }
            }
        }
    )
}

@Preview(showBackground = true) @Composable private fun PreviewNewFav() {
    XeraTheme { NewFavoriteItemDialogCompose("Xera", "https://example.com", {}, {_,_->}) }
}
