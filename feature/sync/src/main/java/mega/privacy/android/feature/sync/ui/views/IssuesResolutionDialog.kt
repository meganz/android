package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun IssuesResolutionDialog(
    @DrawableRes icon: Int,
    conflictName: String,
    nodeName: String,
    actions: List<StalledIssueResolutionAction>,
    actionSelected: (action: StalledIssueResolutionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        FolderHeader(modifier, icon, conflictName, nodeName)

        Divider(
            Modifier.padding(start = 16.dp)
        )

        actions.forEachIndexed { index: Int, action: StalledIssueResolutionAction ->
            IssueResolutionAction(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                actionName = action.actionName,
                actionSelected = {
                    actionSelected(action)
                }
            )
            if (index != actions.size - 1) {
                Divider(
                    Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderHeader(
    modifier: Modifier,
    icon: Int,
    conflictName: String,
    nodeName: String,
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
            MegaText(
                text = conflictName,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME),
                style = MaterialTheme.typography.subtitle1,
            )
            MegaText(
                text = nodeName,
                textColor = TextColor.Secondary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .testTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME),
                style = MaterialTheme.typography.subtitle2
            )
        }
    }
}

@Composable
private fun IssueResolutionAction(
    actionName: String,
    actionSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { actionSelected() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = actionName,
            textColor = TextColor.Primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun IssuesResolutionDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        IssuesResolutionDialog(
            icon = iconPackR.drawable.ic_generic_medium_solid,
            conflictName = "Conflict A",
            nodeName = "some file",
            actions = listOf(
                StalledIssueResolutionAction(
                    "Choose local file",
                    StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
                ), StalledIssueResolutionAction(
                    "Choose remote file",
                    StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE
                )
            ),
            actionSelected = { _ ->
            }
        )
    }
}