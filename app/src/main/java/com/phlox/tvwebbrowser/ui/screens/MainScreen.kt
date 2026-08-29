package com.phlox.tvwebbrowser.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.components.ActionBarCompose
import com.phlox.tvwebbrowser.ui.components.CursorMenuCompose
import com.phlox.tvwebbrowser.ui.components.TabUi
import com.phlox.tvwebbrowser.ui.components.TabsBarCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.widgets.cursor.CursorLayout

/**
 * Compose for activity_main.xml — shell with WebEngine AndroidView
 * Keeps WebView/GeckoView as AndroidView inside CursorLayout for TV focus/cursor
 */
@Composable
fun MainScreen(
    url: String = "https://example.com",
    tabs: List<TabUi> = listOf(TabUi(1, "Xera", true)),
    isMenuVisible: Boolean = false,
    thumbnail: Bitmap? = null,
    isProgressVisible: Boolean = false,
    progress: Int = 0,
    isGenericLoading: Boolean = false,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    isShieldsOn: Boolean = true,
    blockedCount: Int = 3,
    blockedPopups: Int = 0,
    isCursorMenuVisible: Boolean = false,
    cursorLayout: CursorLayout? = null,
    onCursorLayoutCreated: (CursorLayout) -> Unit = {},
    hasExternalContainer: Boolean = false,
    onUrlChanged: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onMenu: () -> Unit = {},
    onVoice: () -> Unit = {},
    onHistory: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onIncognito: () -> Unit = {},
    onSettings: () -> Unit = {},
    onTabSelected: (TabUi) -> Unit = {},
    onTabClose: (TabUi) -> Unit = {},
    onAddTab: () -> Unit = {},
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onCloseTab: () -> Unit = {},
    onHome: () -> Unit = {},
    onAdBlock: () -> Unit = {},
    onPopupBlock: () -> Unit = {},
    onGrab: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onContextMenu: () -> Unit = {},
    onDpad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // When hasExternalContainer is true, Activity hosts CursorLayout via FrameLayout outside Compose (more stable, fixes white-screen crash)
    // In that mode, Compose is transparent overlay only
    val boxBg = if (hasExternalContainer) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxSize().background(boxBg)) {
        // WebEngine container — only when not externally hosted
        if (!hasExternalContainer) {
            AndroidView(
                factory = { ctx ->
                    val layout = cursorLayout ?: CursorLayout(ctx)
                    onCursorLayoutCreated(layout)
                    layout
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Horizontal progress bar at top (3dp)
        if (isProgressVisible) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Generic loading spinner centered
        if (isGenericLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Top bar: ActionBar + Tabs (animated)
        AnimatedVisibility(
            visible = isMenuVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            ) {
                ActionBarCompose(
                    url = url,
                    onUrlChanged = onUrlChanged,
                    onSearch = onSearch,
                    onMenu = onMenu,
                    onVoice = onVoice,
                    onHistory = onHistory,
                    onFavorites = onFavorites,
                    onDownloads = onDownloads,
                    onIncognito = onIncognito,
                    onSettings = onSettings
                )
                TabsBarCompose(tabs = tabs, onTabSelected = onTabSelected, onTabClose = onTabClose, onAddTab = onAddTab)
            }
        }

        // Thumbnail placeholder when menu visible (center)
        if (isMenuVisible) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 120.dp, bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_add_box_24),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.open_a_new_tab_here),
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        // Bottom panel + Shields pill (animated)
        AnimatedVisibility(
            visible = isMenuVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (isShieldsOn) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "SHIELDS ON — Xera",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Bottom panel — 7 buttons in a row, badges for adblock/popup
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCloseTab) {
                            Icon(painterResource(R.drawable.ic_close_grey_900_24dp), contentDescription = stringResource(R.string.close_tab))
                        }
                        IconButton(onClick = onBack, enabled = canGoBack) {
                            Icon(painterResource(R.drawable.back_icon_selector), contentDescription = stringResource(R.string.navigate_back))
                        }
                        IconButton(onClick = onForward, enabled = canGoForward) {
                            Icon(painterResource(R.drawable.forward_icon_selector), contentDescription = stringResource(R.string.navigate_forward))
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(painterResource(R.drawable.ic_refresh_grey_900_24dp), contentDescription = stringResource(R.string.refresh_page))
                        }
                        Box {
                            IconButton(onClick = onAdBlock) {
                                Icon(
                                    painterResource(if (isShieldsOn) R.drawable.ic_adblock_on else R.drawable.ic_adblock_off),
                                    contentDescription = stringResource(R.string.toggle_ads_blocking)
                                )
                            }
                            if (isShieldsOn && blockedCount > 0) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    containerColor = MaterialTheme.colorScheme.error
                                ) { Text("$blockedCount", fontSize = 10.sp) }
                            }
                        }
                        Box {
                            IconButton(onClick = onPopupBlock) {
                                Icon(painterResource(R.drawable.ic_block_popups), contentDescription = stringResource(R.string.block_popups))
                            }
                            if (blockedPopups > 0) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    containerColor = MaterialTheme.colorScheme.error
                                ) { Text("$blockedPopups", fontSize = 10.sp) }
                            }
                        }
                        IconButton(onClick = onHome) {
                            Icon(painterResource(R.drawable.ic_home_grey_900_24dp), contentDescription = stringResource(R.string.navigate_home))
                        }
                    }
                }
            }
        }

        // Cursor menu overlay centered
        AnimatedVisibility(
            visible = isCursorMenuVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp, shadowElevation = 8.dp) {
                CursorMenuCompose(onGrab = onGrab, onZoomIn = onZoomIn, onZoomOut = onZoomOut, onMenu = onContextMenu, onDpad = onDpad)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewMain() {
    XeraTheme { MainScreen(isMenuVisible = true) }
}
@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewMainBrowsing() {
    XeraTheme { MainScreen(isMenuVisible = false, isProgressVisible = true, progress = 42) }
}
