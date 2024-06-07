package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TransparentChipStyle
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Composable that represents the tags view in the file info screen.
 *
 * @param tags List of tags to be displayed.
 * @param onAddTagClick Callback to be called when the user clicks on the add tag view.
 * @param onRemoveTagClick Callback to be called when the user clicks on a tag to remove it.
 * @param modifier Modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileInfoTagsView(
    tags: List<String>,
    onAddTagClick: () -> Unit,
    onRemoveTagClick: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clickable { onAddTagClick() },
    ) {
        MenuActionListTile(
            text = "Tags",
            dividerType = null,
            addIconPadding = false,
            trailingItem = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "Add Tag",
                    modifier = Modifier.size(24.dp),
                )
            },
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(tags.size) { tag ->
                MegaChip(
                    selected = false,
                    text = "#${tags[tag]}",
                    contentDescription = "",
                    trailingIcon = mega.privacy.android.core.R.drawable.ic_universal_close,
                    onClick = { onRemoveTagClick(tags[tag]) },
                    enabled = true,
                    style = TransparentChipStyle,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FileInfoTagsViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FileInfoTagsView(
            tags = listOf("josgh", "skljdaökldj", "Tag 1", "Tag 2", "Tag 3", "Tag 4", "Tag 5"),
            onAddTagClick = {},
            onRemoveTagClick = {},
            modifier = Modifier
        )
    }
}
