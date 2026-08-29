package com.phlox.tvwebbrowser.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.phlox.tvwebbrowser.ui.components.ActionBarCompose
import com.phlox.tvwebbrowser.ui.components.CursorMenuCompose
import com.phlox.tvwebbrowser.ui.components.TabUi
import com.phlox.tvwebbrowser.ui.components.TabsBarCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.widgets.cursor.CursorLayout

/**
 * Compose for activity_main.xml — shell with WebEngine AndroidView
 * Keeps WebView/GeckoView as AndroidView for TV focus/JS, rest is Compose
 */
@Composable
fun MainScreen(
    url: String = "https://example.com",
    tabs: List<TabUi> = listOf(TabUi(1, "Xera", true)),
    isShieldsOn: Boolean = true,
    blockedCount: Int = 3,
    onUrlChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Column {
                ActionBarCompose(url = url, onUrlChanged = onUrlChanged, onSearch = {}, onMenu = {}, onVoice = {}, onHistory = {}, onFavorites = {}, onDownloads = {}, onIncognito = {}, onSettings = {})
                TabsBarCompose(tabs = tabs, onTabSelected = {}, onTabClose = {}, onAddTab = {})
            }
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {}) { Text("Back") }
                Button(onClick = {}) { Text("Refresh") }
                Badge { Text(if (isShieldsOn) "$blockedCount" else "0") }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // WebEngine container — AndroidView for CursorLayout (TV cursor + WebView)
            AndroidView(
                factory = { ctx ->
                    CursorLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isShieldsOn) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 68.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) { Text("SHIELDS ON — Xera", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
            }
            CursorMenuCompose(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewMain() {
    XeraTheme { MainScreen() }
}
