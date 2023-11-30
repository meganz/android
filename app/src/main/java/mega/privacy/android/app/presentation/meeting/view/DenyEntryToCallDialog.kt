package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog

/**
 * Show the dialog to deny a user entry to the call.
 *
 * @param onDenyEntryClick                          To be triggered when positive button is pressed
 * @param onCancelDenyEntryClick                    To be triggered when negative button is pressed
 * @param onDismiss                                 To be triggered when deny participant dialog is hidden
 */
@Composable
fun DenyEntryToCallDialog(
    state: WaitingRoomManagementState,
    onDenyEntryClick: () -> Unit = {},
    onCancelDenyEntryClick: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    if (state.usersInWaitingRoomIDs.isNotEmpty() && !state.showParticipantsInWaitingRoomDialog && state.showDenyParticipantDialog) {
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
            text = message,
            dismissOnClickOutside = true,
            dismissOnBackPress = false,
            confirmButtonText = stringResource(
                id = R.string.meetings_waiting_room_deny_user_to_call_dialog_button
            ),
            onConfirm = onDenyEntryClick,
            cancelButtonText = stringResource(
                id = R.string.meetings_waiting_room_do_not_deny_user_to_call_dialog_button
            ),
            onCancel = onCancelDenyEntryClick,
            onDismiss = onDismiss,
        )
    }
}

/**
 * [DenyEntryToCallDialog] preview
 */
@Preview
@Composable
fun PreviewDenyEntryToCallDialog() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DenyEntryToCallDialog(
            WaitingRoomManagementState(
                scheduledMeetingTitle = "Title",
                nameOfTheFirstUserInTheWaitingRoom = "Name",
                showParticipantsInWaitingRoomDialog = true
            ),
            onDenyEntryClick = {},
            onCancelDenyEntryClick = {},
            onDismiss = {}
        )
    }
}

