package com.phlox.tvwebbrowser.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

data class TabUi(val id: Long, val title: String, val selected: Boolean)

@Composable
fun TabsBarCompose(
    tabs: List<TabUi>,
    onTabSelected: (TabUi) -> Unit,
    onTabClose: (TabUi) -> Unit,
    onAddTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        LazyRow(modifier = Modifier.weight(1f)) {
            items(tabs, key = { it.id }) { tab ->
                WebTabChip(
                    title = tab.title,
                    selected = tab.selected,
                    onSelected = { onTabSelected(tab) },
                    onClose = { onTabClose(tab) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        Button(onClick = onAddTab, modifier = Modifier.padding(start = 5.dp)) { Text("+") }
    }
}
@Preview(showBackground = true) @Composable private fun PreviewTabs() {
    XeraTheme { TabsBarCompose(listOf(TabUi(1,"Xera",true), TabUi(2,"Reddit",false)), {}, {}, {}) }
}
