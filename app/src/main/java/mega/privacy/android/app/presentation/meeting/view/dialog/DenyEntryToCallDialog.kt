package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Show the dialog to deny a user entry to the call.
 */
@Composable
fun DenyEntryToCallDialog(
    viewModel: WaitingRoomManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    DenyEntryToCallDialog(
        uiState = uiState,
        onDenyEntryClick = viewModel::denyEntryClick,
        onCancelDenyEntryClick = viewModel::cancelDenyEntryClick,
        onDismiss = viewModel::setShowDenyParticipantDialogConsumed
    )
}

/**
 * Show the dialog to deny a user entry to the call.
 */
@Composable
@Deprecated("Only required for UsersInWaitingRoomDialogFragment. Remove this once it is finally removed.")
fun DenyEntryToCallDialog(
    onDismiss: () -> Unit,
    viewModel: WaitingRoomManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    DenyEntryToCallDialog(
        uiState = uiState,
        onDenyEntryClick = {
            viewModel.denyEntryClick()
            onDismiss()
        },
        onCancelDenyEntryClick = viewModel::cancelDenyEntryClick,
        onDismiss = {
            viewModel.setShowDenyParticipantDialogConsumed()
            onDismiss()
        }
    )
}

/**
 * Show the dialog to deny a user entry to the call.
 *
 * @param onDenyEntryClick                          To be triggered when positive button is pressed
 * @param onCancelDenyEntryClick                    To be triggered when negative button is pressed
 * @param onDismiss                                 To be triggered when deny participant dialog is hidden
 */
@Composable
private fun DenyEntryToCallDialog(
    uiState: WaitingRoomManagementState,
    onDenyEntryClick: () -> Unit = {},
    onCancelDenyEntryClick: () -> Unit = {},
    onDismiss: () -> Unit,
) = with(uiState) {
    if (usersInWaitingRoomIDs.isNotEmpty() && !showParticipantsInWaitingRoomDialog && showDenyParticipantDialog) {
        var name = nameOfTheFirstUserInTheWaitingRoom
        participantToDenyEntry?.apply {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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

