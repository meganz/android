package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Show there are participants waiting in the waiting room dialog
 *
 * @param onAdmitClick                              To be triggered when positive button is pressed
 * @param onSeeWaitingRoomClick                     To be triggered when negative button is pressed
 * @param onDenyClick                               To be triggered when negative button is pressed
 * @param onDismiss                                 To be triggered when admit participants dialog is hidden
 */
@Composable
fun UsersInWaitingRoomDialog(
    state: WaitingRoomManagementState,
    onAdmitClick: () -> Unit,
    onSeeWaitingRoomClick: () -> Unit,
    onDismiss: () -> Unit,
    onDenyClick: () -> Unit = {},
) {
    if (state.usersInWaitingRoomIDs.isNotEmpty() && state.showParticipantsInWaitingRoomDialog && !state.showDenyParticipantDialog && !state.isWaitingRoomSectionOpened) {
        val isOneParticipantInWaitingRoom = state.usersInWaitingRoomIDs.size == 1
        val isDialogRelativeToTheOpenCall = state.isDialogRelativeToTheOpenCall()
        val usersSize = state.usersInWaitingRoomIDs.size

        val message =
            when {
                isDialogRelativeToTheOpenCall && isOneParticipantInWaitingRoom && state.nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() -> stringResource(
                    R.string.meetings_waiting_room_admit_user_to_call_dialog_message,
                    state.nameOfTheFirstUserInTheWaitingRoom
                )

                isDialogRelativeToTheOpenCall && !isOneParticipantInWaitingRoom -> pluralStringResource(
                    id = R.plurals.meetings_waiting_room_admit_users_to_call_dialog_message,
                    count = usersSize,
                    usersSize
                )

                !isDialogRelativeToTheOpenCall && isOneParticipantInWaitingRoom && state.nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() -> stringResource(
                    R.string.meetings_waiting_room_admit_user_to_call_outside_call_screen_dialog_message,
                    state.nameOfTheFirstUserInTheWaitingRoom, state.scheduledMeetingTitle
                )

                else -> pluralStringResource(
                    id = R.plurals.meetings_waiting_room_admit_users_to_call_outside_call_screen_dialog_message,
                    count = state.usersInWaitingRoomIDs.size,
                    state.usersInWaitingRoomIDs.size,
                    state.scheduledMeetingTitle
                )
            }

        MegaAlertDialog(
            title = null,
            text = message,
            dismissOnClickOutside = true,
            dismissOnBackPress = false,
            confirmButtonText = stringResource(
                id = when {
                    isOneParticipantInWaitingRoom -> R.string.meetings_waiting_room_admit_user_to_call_dialog_admit_button
                    else -> R.string.meetings_waiting_room_admit_users_to_call_dialog_see_waiting_room_button
                }
            ),
            onConfirm = when {
                isOneParticipantInWaitingRoom -> onAdmitClick
                else -> onSeeWaitingRoomClick
            },
            cancelButtonText = stringResource(
                id = when {
                    isOneParticipantInWaitingRoom -> R.string.meetings_waiting_room_admit_users_to_call_dialog_deny_button
                    else -> R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button
                }

            ),
            onCancel = when {
                isOneParticipantInWaitingRoom -> onDenyClick
                else -> onAdmitClick
            },
            onDismiss = onDismiss,
        )
    }
}

/**
 * [UsersInWaitingRoomDialog] preview
 */
@Preview
@Composable
fun PreviewUsersInWaitingRoomDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        UsersInWaitingRoomDialog(
            WaitingRoomManagementState(
                scheduledMeetingTitle = "Title",
                nameOfTheFirstUserInTheWaitingRoom = "Name",
                showParticipantsInWaitingRoomDialog = true
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDismiss = {},
        )
    }
}

/**
 * [UsersInWaitingRoomDialog] preview
 */
@Preview
@Composable
fun PreviewDenyUserInWaitingRoomDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        UsersInWaitingRoomDialog(
            WaitingRoomManagementState(
                scheduledMeetingTitle = "Title",
                nameOfTheFirstUserInTheWaitingRoom = "User1",
                nameOfTheSecondUserInTheWaitingRoom = "User2",
                showParticipantsInWaitingRoomDialog = false,
                showDenyParticipantDialog = true
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDenyClick = {},
            onDismiss = {},
        )
    }
}

