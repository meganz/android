package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
 * Show there are participants waiting in the waiting room dialog.
 */
@Composable
fun UsersInWaitingRoomDialog(
    viewModel: WaitingRoomManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    UsersInWaitingRoomDialog(
        uiState = uiState,
        onAdmitClick = viewModel::admitUsersClick,
        onSeeWaitingRoomClick = viewModel::seeWaitingRoomClick,
        onDenyClick = viewModel::denyUsersClick,
        onDismiss = viewModel::setShowParticipantsInWaitingRoomDialogConsumed,
    )
}

/**
 * Show there are participants waiting in the waiting room dialog.
 */
@Deprecated("Only required for UsersInWaitingRoomDialogFragment. Remove this once it is finally removed.")
@Composable
fun UsersInWaitingRoomDialog(
    onSeeWaitingRoomClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: WaitingRoomManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    UsersInWaitingRoomDialog(
        uiState = uiState,
        onAdmitClick = viewModel::admitUsersClick,
        onSeeWaitingRoomClick = onSeeWaitingRoomClick,
        onDenyClick = viewModel::denyUsersClick,
        onDismiss = {
            viewModel.setShowParticipantsInWaitingRoomDialogConsumed()
            onDismiss()
        },
    )
}

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
    uiState: WaitingRoomManagementState,
    onAdmitClick: () -> Unit,
    onSeeWaitingRoomClick: () -> Unit,
    onDismiss: () -> Unit,
    onDenyClick: () -> Unit = {},
) = with(uiState) {
    if (usersInWaitingRoomIDs.isNotEmpty() && showParticipantsInWaitingRoomDialog && !showDenyParticipantDialog && !isWaitingRoomSectionOpened) {
        val isOneParticipantInWaitingRoom = usersInWaitingRoomIDs.size == 1
        val isDialogRelativeToTheOpenCall = isDialogRelativeToTheOpenCall()
        val usersSize = usersInWaitingRoomIDs.size

        val message =
            when {
                isDialogRelativeToTheOpenCall && isOneParticipantInWaitingRoom && nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() -> stringResource(
                    R.string.meetings_waiting_room_admit_user_to_call_dialog_message,
                    nameOfTheFirstUserInTheWaitingRoom
                )

                isDialogRelativeToTheOpenCall && !isOneParticipantInWaitingRoom -> pluralStringResource(
                    id = R.plurals.meetings_waiting_room_admit_users_to_call_dialog_message,
                    count = usersSize,
                    usersSize
                )

                !isDialogRelativeToTheOpenCall && isOneParticipantInWaitingRoom && nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() -> stringResource(
                    R.string.meetings_waiting_room_admit_user_to_call_outside_call_screen_dialog_message,
                    nameOfTheFirstUserInTheWaitingRoom, scheduledMeetingTitle
                )

                else -> pluralStringResource(
                    id = R.plurals.meetings_waiting_room_admit_users_to_call_outside_call_screen_dialog_message,
                    count = usersInWaitingRoomIDs.size,
                    usersInWaitingRoomIDs.size,
                    scheduledMeetingTitle
                )
            }

        val body: String? = when {
            showUserLimitWarning -> {
                stringResource(R.string.meetings_free_call_organiser_number_of_participants_warning)
            }

            else -> null
        }

        MegaAlertDialog(
            modifier = Modifier.testTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG),
            title = message,
            body = body,
            confirmEnabled = when {
                isOneParticipantInWaitingRoom -> !showUserLimitWarning
                else -> true
            },
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

            cancelEnabled = when {
                isOneParticipantInWaitingRoom -> true
                else -> !isAdmitAllButtonDisabled
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
            dismissOnClickOutside = true,
            dismissOnBackPress = false
        )
    }
}

/**
 * [UsersInWaitingRoomDialog] preview
 */
@Preview
@Composable
fun PreviewUsersInWaitingRoomDialog() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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

internal const val TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG =
    "meeting:users_in_waiting_room_dialog:dialog"


