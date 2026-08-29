package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun WebTabChip(title: String, selected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.width(340.dp).padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(40.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 3.dp)
        )
    }
}
@Preview(showBackground = true) @Composable private fun PreviewChip() { XeraTheme { WebTabChip("Xera — Home", true) } }
