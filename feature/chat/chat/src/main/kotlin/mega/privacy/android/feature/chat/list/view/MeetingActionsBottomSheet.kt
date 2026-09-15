package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.chat.list.model.MeetingAction
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Modal bottom sheet offering the meeting actions reachable from the Meetings tab.
 *
 * The sheet hides itself before the callback runs, so the hosting screen sees the picked action
 * only once the sheet is gone.
 *
 * @param onActionClick Called with the picked action after the sheet has hidden.
 * @param onDismiss Called when the sheet is dismissed without picking an action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetingActionsBottomSheet(
    onActionClick: (MeetingAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val close = { action: MeetingAction ->
        coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                onDismiss()
                onActionClick(action)
            }
        Unit
    }

    MegaModalBottomSheet(
        modifier = Modifier.testTag(MEETING_ACTIONS_SHEET_TAG),
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        MeetingActionsSheetContent(onActionClick = close)
    }
}

/**
 * Stateless list of meeting action rows rendered inside the modal bottom sheet.
 *
 * @param onActionClick Called with the picked action.
 * @param modifier [Modifier]
 */
@Composable
internal fun MeetingActionsSheetContent(
    onActionClick: (MeetingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MeetingActionItem(
            text = stringResource(sharedR.string.chat_meeting_actions_start_now),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.VideoPlus),
            testTag = MEETING_ACTIONS_START_TAG,
            onClick = { onActionClick(MeetingAction.Start) },
        )
        MeetingActionItem(
            text = stringResource(sharedR.string.chat_meeting_actions_join),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.VideoJoin),
            testTag = MEETING_ACTIONS_JOIN_TAG,
            onClick = { onActionClick(MeetingAction.Join) },
        )
        MeetingActionItem(
            text = stringResource(sharedR.string.chat_meeting_actions_schedule),
            painter = painterResource(IconR.drawable.ic_calendar_01_medium_thin_outline),
            testTag = MEETING_ACTIONS_SCHEDULE_TAG,
            onClick = { onActionClick(MeetingAction.Schedule) },
        )
    }
}

@Composable
private fun MeetingActionItem(
    text: String,
    painter: Painter,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            painter = painter,
            tint = IconColor.Secondary,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        MegaText(
            text = text,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 32.dp),
        )
    }
}

internal const val MEETING_ACTIONS_SHEET_TAG = "meeting_actions_sheet:sheet"
internal const val MEETING_ACTIONS_START_TAG = "meeting_actions_sheet:start_meeting"
internal const val MEETING_ACTIONS_JOIN_TAG = "meeting_actions_sheet:join_meeting"
internal const val MEETING_ACTIONS_SCHEDULE_TAG = "meeting_actions_sheet:schedule_meeting"

@CombinedThemePreviews
@Composable
private fun MeetingActionsSheetContentPreview() {
    AndroidThemeForPreviews {
        MeetingActionsSheetContent(onActionClick = {})
    }
}
