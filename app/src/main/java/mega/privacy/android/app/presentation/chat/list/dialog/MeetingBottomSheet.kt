package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Compose replacement for `MeetingBottomSheetDialogFragment`.
 *
 * Renders the three meeting actions (Start / Join / Schedule) as a modal bottom sheet.
 *
 * @param onStartMeeting Called when "Start meeting" is tapped.
 * @param onJoinMeeting Called when "Join meeting" is tapped.
 * @param onScheduleMeeting Called when "Schedule meeting" is tapped.
 * @param onDismissRequest Called when the bottom sheet should be dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingBottomSheet(
    onStartMeeting: () -> Unit,
    onJoinMeeting: () -> Unit,
    onScheduleMeeting: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded },
    )

    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissRequest,
    ) {
        MeetingBottomSheetContent(
            onStartMeetingClick = {
                onStartMeeting()
                onDismissRequest()
            },
            onJoinMeetingClick = {
                onJoinMeeting()
                onDismissRequest()
            },
            onScheduleMeetingClick = {
                onScheduleMeeting()
                onDismissRequest()
            },
        )
    }
}

/**
 * Stateless content of [MeetingBottomSheet]. Renders the three menu tiles so it can be
 * exercised in `@Preview` and unit tests without a bottom-sheet host.
 */
@Composable
internal fun MeetingBottomSheetContent(
    onStartMeetingClick: () -> Unit = {},
    onJoinMeetingClick: () -> Unit = {},
    onScheduleMeetingClick: () -> Unit = {},
) {
    Column {
        MenuActionListTile(
            modifier = Modifier.testTag(TEST_TAG_START_MEETING),
            text = stringResource(R.string.action_start_meeting_now),
            icon = painterResource(IconR.drawable.ic_video_plus_medium_thin_outline),
            dividerType = DividerType.BigStartPadding,
            onActionClicked = onStartMeetingClick,
        )
        MenuActionListTile(
            modifier = Modifier.testTag(TEST_TAG_JOIN_MEETING),
            text = stringResource(R.string.join_meeting),
            icon = painterResource(IconR.drawable.ic_video_join_medium_thin_outline),
            dividerType = DividerType.BigStartPadding,
            onActionClicked = onJoinMeetingClick,
        )
        MenuActionListTile(
            modifier = Modifier.testTag(TEST_TAG_SCHEDULE_MEETING),
            text = stringResource(R.string.chat_schedule_meeting),
            icon = painterResource(IconR.drawable.ic_calendar_01_medium_thin_outline),
            dividerType = null,
            onActionClicked = onScheduleMeetingClick,
        )
    }
}

internal const val TEST_TAG_START_MEETING = "meeting_bottom_sheet:start_meeting"
internal const val TEST_TAG_JOIN_MEETING = "meeting_bottom_sheet:join_meeting"
internal const val TEST_TAG_SCHEDULE_MEETING = "meeting_bottom_sheet:schedule_meeting"

@CombinedThemeComponentPreviews
@Composable
private fun MeetingBottomSheetContentPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MeetingBottomSheetContent()
    }
}
