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
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chip.Chip
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

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
    Column(modifier = modifier
        .padding(vertical = 8.dp)
        .clickable { onAddTagClick() }) {
        MegaText(
            text = "Tags",
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption,
        )
        GenericDescriptionTextField(
            value = "#Add tags",
            modifier = Modifier
                .fillMaxWidth(),
            onValueChange = {},
            isEnabled = false,
            placeholderId = R.string.meetings_schedule_meeting_add_description_label,
            showUnderline = true,
            charLimit = 32
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(tags.size) { tag ->
                Chip(
                    selected = false,
                    enabled = true,
                    contentDescription = "Tag Chip",
                    showTransparentBackground = true,
                    onClick = { onRemoveTagClick(tags[tag]) },
                ) {
                    MegaText(
                        text = "#${tags[tag]}",
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle2
                    )
                    Icon(
                        modifier = Modifier
                            .testTag(FILE_INFO_CHIP_CLOSE_ICON_TEST_TAG)
                            .size(18.dp),
                        imageVector = ImageVector.vectorResource(id = mega.privacy.android.core.R.drawable.ic_universal_close),
                        contentDescription = "Choose Options",
                    )
                }
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

/**
 * Test tag for the tags view.
 */
internal const val FILE_INFO_CHIP_CLOSE_ICON_TEST_TAG = "file_info_chips:close_icon"
