package mega.privacy.android.shared.original.core.ui.controls.chip

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * ChipBar is a scrollable toolbar for chips
 *
 * @param modifier Modifier for chip bar
 * @param scrollState ScrollState for chip bar
 * @param content scroll area for chips
 */
@Composable
fun ChipBar(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@CombinedThemePreviews
@Composable
private fun ChipBarPreview() {
    val chips = listOf("Documents", "Photos", "Videos", "Important Files", "Backup")
    val selectedChips = remember { mutableStateListOf(chips[0]) }

    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChipBar {
            chips.forEach { item ->
                val isSelected = selectedChips.contains(item)
                MegaChip(
                    selected = isSelected,
                    text = item,
                    onClick = {
                        if (isSelected) {
                            selectedChips.remove(item)
                        } else {
                            selectedChips.add(item)
                        }
                    },
                )
            }
        }
    }
}
