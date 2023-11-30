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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType

@Composable
internal fun IssuesResolutionDialog(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    conflictName: String,
    nodeName: String,
    actions: List<StalledIssueResolutionAction>,
    actionSelected: (action: StalledIssueResolutionAction) -> Unit,
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
        Text(
            text = actionName,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary)
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun IssuesResolutionDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        IssuesResolutionDialog(
            icon = iconPackR.drawable.ic_generic_list,
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