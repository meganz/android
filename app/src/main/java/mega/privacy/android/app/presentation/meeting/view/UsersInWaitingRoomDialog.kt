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
 * @param onDenyEntryClick                          To be triggered when positive button is pressed
 * @param onDismiss                                 To be triggered when admit participants dialog is hidden
 * @param onCancelDenyEntryClick                    To be triggered when deny participant dialog is hidden
 */
@Composable
fun UsersInWaitingRoomDialog(
    state: WaitingRoomManagementState,
    onAdmitClick: () -> Unit,
    onSeeWaitingRoomClick: () -> Unit,
    onDismiss: () -> Unit,
    onDenyClick: () -> Unit = {},
    onDenyEntryClick: () -> Unit = {},
    onCancelDenyEntryClick: () -> Unit = {},
) {
    if (state.usersInWaitingRoom.isNotEmpty() && (state.showParticipantsInWaitingRoomDialog || state.showDenyParticipantDialog)) {
        val isOneParticipantInWaitingRoom = state.usersInWaitingRoom.size == 1
        val isDialogRelativeToTheOpenCall = state.isDialogRelativeToTheOpenCall()
        val usersSize = state.usersInWaitingRoom.size

        val message =
            when {
                state.showParticipantsInWaitingRoomDialog -> when {
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
                        count = state.usersInWaitingRoom.size,
                        state.usersInWaitingRoom.size,
                        state.scheduledMeetingTitle
                    )
                }

                else -> {
                    var name = state.nameOfTheFirstUserInTheWaitingRoom
                    state.participantToDenyEntry?.apply {
                        data.fullName?.let {
                            name = it
                        }
                    }
                    stringResource(
                        R.string.meetings_waiting_room_deny_user_to_call_dialog_message, name
                    )
                }
            }

        MegaAlertDialog(
            title = null,
            text = message,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            confirmButtonText = stringResource(
                id =
                when (state.showDenyParticipantDialog) {
                    true -> R.string.meetings_waiting_room_deny_user_to_call_dialog_button
                    false -> when {
                        isOneParticipantInWaitingRoom -> R.string.meetings_waiting_room_admit_user_to_call_dialog_admit_button
                        else -> R.string.meetings_waiting_room_admit_users_to_call_dialog_see_waiting_room_button
                    }
                }
            ),
            onConfirm = when (state.showDenyParticipantDialog) {
                true -> onDenyEntryClick
                false -> when {
                    isOneParticipantInWaitingRoom -> onAdmitClick
                    else -> onSeeWaitingRoomClick
                }
            },
            cancelButtonText = stringResource(
                id =
                when (state.showDenyParticipantDialog) {
                    true -> R.string.general_cancel
                    false -> when {
                        isOneParticipantInWaitingRoom -> R.string.meetings_waiting_room_admit_users_to_call_dialog_deny_button
                        else -> R.string.meetings_waiting_room_admit_users_to_call_dialog_admit_button
                    }
                }
            ),
            onCancel =
            when (state.showDenyParticipantDialog) {
                true -> onCancelDenyEntryClick
                false -> when {
                    isOneParticipantInWaitingRoom -> onDenyClick
                    else -> onAdmitClick
                }
            },
            onDismiss = if (state.showDenyParticipantDialog) onCancelDenyEntryClick else onDismiss,
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
            onCancelDenyEntryClick = {}
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
            onCancelDenyEntryClick = {},
            onDismiss = {},
        )
    }
}

