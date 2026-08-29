package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

@Composable
fun VoiceResultBar(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp) {
        Row(modifier = Modifier.height(50.dp).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_mic_none_grey_900_36dp), contentDescription = null, modifier = Modifier.size(42.dp).padding(start = 5.dp))
            Text(text, fontSize = 20.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}
@Preview(showBackground = true) @Composable private fun PreviewVoice() { XeraTheme { VoiceResultBar("Hello Xera") } }
