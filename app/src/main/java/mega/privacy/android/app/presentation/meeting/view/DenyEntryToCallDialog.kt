package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Show the dialog to deny a user entry to the call.
 *
 * @param onDenyEntryClick                          To be triggered when positive button is pressed
 * @param onCancelDenyEntryClick                    To be triggered when deny participant dialog is hidden
 */
@Composable
fun DenyEntryToCallDialog(
    state: WaitingRoomManagementState,
    onDenyEntryClick: () -> Unit = {},
    onCancelDenyEntryClick: () -> Unit = {},
) {
    if (state.usersInWaitingRoom.isNotEmpty() && !state.showParticipantsInWaitingRoomDialog && state.showDenyParticipantDialog) {
        var name = state.nameOfTheFirstUserInTheWaitingRoom
        state.participantToDenyEntry?.apply {
            data.fullName?.let {
                name = it
            }
        }

        val message = stringResource(
            R.string.meetings_waiting_room_deny_user_to_call_dialog_message, name
        )

        MegaAlertDialog(
            title = null,
            text = message,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            confirmButtonText = stringResource(
                id = R.string.meetings_waiting_room_deny_user_to_call_dialog_button
            ),
            onConfirm = onDenyEntryClick,
            cancelButtonText = stringResource(
                id = R.string.meetings_waiting_room_do_not_deny_user_to_call_dialog_button
            ),
            onCancel = onCancelDenyEntryClick,
            onDismiss = onCancelDenyEntryClick,
        )
    }
}

/**
 * [DenyEntryToCallDialog] preview
 */
@Preview
@Composable
fun PreviewDenyEntryToCallDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DenyEntryToCallDialog(
            WaitingRoomManagementState(
                scheduledMeetingTitle = "Title",
                nameOfTheFirstUserInTheWaitingRoom = "Name",
                showParticipantsInWaitingRoomDialog = true
            ),
            onDenyEntryClick = {},
            onCancelDenyEntryClick = {}
        )
    }
}

