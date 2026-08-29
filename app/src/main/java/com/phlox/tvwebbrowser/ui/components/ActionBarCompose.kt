package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun ActionBarCompose(
    url: String,
    onUrlChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    onVoice: () -> Unit,
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit,
    onIncognito: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) { Icon(painterResource(R.drawable.ic_close_grey_900_36dp), contentDescription = stringResource(R.string.close_application)) }
        IconButton(onClick = onVoice) { Icon(painterResource(R.drawable.ic_mic_none_grey_900_36dp), contentDescription = stringResource(R.string.voice_search)) }
        IconButton(onClick = onHistory) { Icon(painterResource(R.drawable.ic_history_grey_900_36dp), contentDescription = stringResource(R.string.history)) }
        IconButton(onClick = onFavorites) { Icon(painterResource(R.drawable.ic_star_border_grey_900_36dp), contentDescription = stringResource(R.string.favorites)) }
        IconButton(onClick = onDownloads) { Icon(painterResource(R.drawable.ic_file_download_grey_900), contentDescription = stringResource(R.string.downloads)) }
        IconButton(onClick = onIncognito) { Icon(painterResource(R.drawable.ic_incognito), contentDescription = stringResource(R.string.incognito_mode)) }
        IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_settings_grey_900_24dp), contentDescription = stringResource(R.string.settings)) }
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChanged,
            placeholder = { Text(stringResource(R.string.url_prompt)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f).padding(start = 5.dp)
        )
    }
}
@Preview(showBackground = true, widthDp = 960) @Composable private fun PreviewActionBar() {
    XeraTheme { ActionBarCompose("https://example.com", {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
