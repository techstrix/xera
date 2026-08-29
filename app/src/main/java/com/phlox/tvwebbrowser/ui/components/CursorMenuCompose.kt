package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun CursorMenuCompose(
    onGrab: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onMenu: () -> Unit = {},
    onDpad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(170.dp)) {
        IconButton(onClick = onGrab, modifier = Modifier.align(Alignment.Center)) { Icon(painterResource(R.drawable.ic_circle_plus), null) }
        IconButton(onClick = onZoomIn, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(painterResource(R.drawable.ic_zoom_in_gray_24dp), null) }
        IconButton(onClick = onZoomOut, modifier = Modifier.align(Alignment.CenterStart)) { Icon(painterResource(R.drawable.ic_zoom_out_gray_24dp), null) }
        IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopCenter)) { Icon(painterResource(R.drawable.ic_menu_24), null) }
        IconButton(onClick = onDpad, modifier = Modifier.align(Alignment.BottomCenter)) { Icon(painterResource(R.drawable.gamepad_circle_up_24px), null) }
    }
}
@Preview(showBackground = true) @Composable private fun PreviewCursor() { XeraTheme { CursorMenuCompose() } }
