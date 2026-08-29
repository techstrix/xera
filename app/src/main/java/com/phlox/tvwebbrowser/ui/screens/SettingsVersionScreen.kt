package com.phlox.tvwebbrowser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
fun SettingsVersionScreen(
    version: String,
    buildFlavor: String,
    webViewVersion: String,
    onLinkClick: () -> Unit,
    onSupportClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onUkraineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Xera", fontSize = 20.sp)
        Text("Web browser optimized for TVs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
        Text("https://github.com/techstrix/xera", modifier = Modifier.clickable { onLinkClick() }, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Text("Version: $version", modifier = Modifier.padding(top = 20.dp))
        Text("Build flavor: $buildFlavor")
        Text("WebView Version: $webViewVersion")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Text("Support the author", modifier = Modifier.clickable { onSupportClick() }, color = MaterialTheme.colorScheme.primary)
        }
        Text("License", modifier = Modifier.clickable { onLicenseClick() }, color = MaterialTheme.colorScheme.primary)
        Text("Privacy policy", modifier = Modifier.clickable { onPrivacyClick() }, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Text("Why icon changed?", modifier = Modifier.clickable { onUkraineClick() }, color = MaterialTheme.colorScheme.primary)
            Icon(painterResource(R.drawable.banner), contentDescription = null, modifier = Modifier.size(32.dp).padding(start = 10.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 960, heightDp = 540) @Composable private fun PreviewVersion() {
    XeraTheme { SettingsVersionScreen("2.1.6", "generic · geckoExcluded", "Chrome 120", {}, {}, {}, {}, {}) }
}
