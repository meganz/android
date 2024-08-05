package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TransparentChipStyle
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.NodeInfoTagsEnteredEvent
import mega.privacy.mobile.analytics.event.NodeInfoTagsProOnlyEnteredEvent

/**
 * Composable that represents the tags view in the file info screen.
 *
 * @param tags List of tags to be displayed.
 * @param onAddTagClick Callback to be called when the user clicks on the add tag view.
 * @param modifier Modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileInfoTagsView(
    tags: List<String>,
    onAddTagClick: () -> Unit,
    modifier: Modifier,
    isProAccount: Boolean,
    onUpgradeAccountClick: () -> Unit,
    isBusinessAccountActive: Boolean?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .conditional(isBusinessAccountActive != false) {
                clickable {
                    if (isProAccount || isBusinessAccountActive == true) {
                        onAddTagClick()
                        Analytics.tracker.trackEvent(NodeInfoTagsEnteredEvent)
                    } else {
                        onUpgradeAccountClick()
                        Analytics.tracker.trackEvent(NodeInfoTagsProOnlyEnteredEvent)
                    }
                }
            }
    ) {
        if (isBusinessAccountActive == false) {
            MegaText(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(id = sharedR.string.file_info_information_tags_label),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1,
            )
            MegaText(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(id = sharedR.string.file_info_information_no_tags_label),
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        } else {
            MenuActionListTile(
                text = stringResource(id = sharedR.string.file_info_information_tags_label),
                dividerType = null,
                addIconPadding = false,
                trailingItem = {
                    if (isProAccount || isBusinessAccountActive == true) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = iconPackR.drawable.ic_chevron_right_medium_regular_outline),
                            contentDescription = "Add Tag",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colors.grey_alpha_038_white_alpha_038,
                        )
                    } else if (isBusinessAccountActive == false) {
                        Spacer(modifier = Modifier.size(24.dp))
                    } else {
                        MegaText(
                            text = stringResource(id = R.string.general_pro_only),
                            textColor = TextColor.Accent,
                        )
                    }
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
                        enabled = true,
                        style = TransparentChipStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FileInfoTagsViewPreview(
    @PreviewParameter(BooleanProvider::class) isProAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FileInfoTagsView(
            tags = listOf("Tag", "SampleTag", "Tag1", "Tag2", "Tag3", "Tag4", "Tag5"),
            onAddTagClick = {},
            modifier = Modifier,
            isProAccount = isProAccount,
            onUpgradeAccountClick = {},
            isBusinessAccountActive = isProAccount,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileInfoTagsViewDeactivatedBusinessAccountPreview(
    @PreviewParameter(BooleanProvider::class) isBusinessAccountActive: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FileInfoTagsView(
            tags = listOf("Tag", "SampleTag", "Tag1", "Tag2", "Tag3", "Tag4", "Tag5"),
            onAddTagClick = {},
            modifier = Modifier,
            isProAccount = false,
            onUpgradeAccountClick = {},
            isBusinessAccountActive = isBusinessAccountActive,
        )
    }
}
