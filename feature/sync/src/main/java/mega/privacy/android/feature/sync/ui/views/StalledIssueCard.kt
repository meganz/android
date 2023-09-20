package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun StalledIssueCard(
    nodeName: String,
    conflictName: String,
    @DrawableRes icon: Int,
    issueDetailsClicked: () -> Unit,
    moreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier
            .padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    )
    {
        ThumbnailView(
            modifier = Modifier
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
            Text(
                modifier = Modifier.testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME),
                text = conflictName,
                style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary)
            )
            Text(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME),
                text = nodeName,
                style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary)
            )
        }
        Image(
            modifier = Modifier
                .padding(end = 24.dp)
                .size(24.dp)
                .clickable { issueDetailsClicked() }
                .testTag(TEST_TAG_STALLED_ISSUE_CARD_BUTTON_INFO),
            painter = painterResource(R.drawable.ic_info),
            contentDescription = "Info",
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorPrimary)
        )
        Image(
            modifier = Modifier
                .clickable { moreClicked() }
                .testTag(TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE),
            painter = painterResource(R.drawable.ic_universal_more),
            contentDescription = "More",
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun StalledIssueCardPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        StalledIssueCard(
            nodeName = "Some folder",
            conflictName = "Conflicting name",
            icon = R.drawable.ic_folder_list,
            issueDetailsClicked = {},
            moreClicked = {},
        )
    }
}

internal const val TEST_TAG_STALLED_ISSUE_CARD_ICON_NODE_THUMBNAIL = "stalled_issue_card:icon_node_thumbnail"
internal const val TEST_TAG_STALLED_ISSUE_CARD_BUTTON_INFO = "stalled_issue_card:button_info"
internal const val TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE = "stalled_issue_card:button_more"
internal const val TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME = "stalled_issue_card:text_conflict_name"
internal const val TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME = "stalled_issue_card:text_node_name"

