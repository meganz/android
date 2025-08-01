package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun StalledIssueCard(
    nodeName: String,
    nodePath: String,
    conflictName: String,
    @DrawableRes icon: Int,
    shouldShowMoreIcon: Boolean,
    moreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column {
        Row(
            modifier
                .padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        )
        {
            ThumbnailView(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag(TEST_TAG_STALLED_ISSUE_CARD_ICON_NODE_THUMBNAIL),
                data = null,
                defaultImage = icon,
                contentDescription = "Node thumbnail"
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                MegaText(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME),
                    text = nodeName,
                    style = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Primary
                )
                if (nodePath.isNotEmpty()) {
                    MegaText(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_PATH),
                        text = nodePath,
                        style = MaterialTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary
                    )
                }
                MegaText(
                    modifier = Modifier.testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME),
                    text = conflictName,
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Brand
                )
            }
            MegaIcon(
                modifier = Modifier
                    .clickable(shouldShowMoreIcon) { moreClicked() }
                    .testTag(TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE)
                    .alpha(if (shouldShowMoreIcon) 1f else 0f),
                painter = painterResource(R.drawable.ic_universal_more),
                contentDescription = "More",
                tint = IconColor.Secondary
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun StalledIssueCardPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        StalledIssueCard(
            nodeName = "Some folder",
            nodePath = "Some path",
            conflictName = "Conflicting name",
            icon = IconPackR.drawable.ic_folder_medium_solid,
            shouldShowMoreIcon = true,
            moreClicked = {},
        )
    }
}

internal const val TEST_TAG_STALLED_ISSUE_CARD_ICON_NODE_THUMBNAIL =
    "stalled_issue_card:icon_node_thumbnail"
internal const val TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE = "stalled_issue_card:button_more"
internal const val TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME =
    "stalled_issue_card:text_conflict_name"
internal const val TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME = "stalled_issue_card:text_node_name"
internal const val TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_PATH = "stalled_issue_card:text_node_path"
