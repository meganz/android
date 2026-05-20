package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.chatstatus.ChatStatusViewModel
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Compose replacement for `ChatStatusDialogFragment`.
 *
 * @param onDismissRequest Called when the dialog should be dismissed.
 * @param onError Called when the status change fails — host should show an error snackbar.
 */
@Composable
internal fun ChatStatusDialog(
    viewModel: ChatStatusViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
    onError: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.result) {
        uiState.result?.let { result ->
            if (result.isFailure) onError()
            onDismissRequest()
        }
    }

    ChatStatusDialogContent(
        currentStatus = uiState.status,
        onOptionSelected = viewModel::setUserStatus,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
internal fun ChatStatusDialogContent(
    currentStatus: UserChatStatus,
    onOptionSelected: (UserChatStatus) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmationDialogWithRadioButtons(
        radioOptions = UserChatStatusOptions,
        initialSelectedOption = currentStatus,
        titleText = stringResource(id = R.string.status_label),
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
        optionDescriptionMapper = { userStatusText(it) },
    )
}

private val UserChatStatusOptions = listOf(
    UserChatStatus.Online,
    UserChatStatus.Away,
    UserChatStatus.Busy,
    UserChatStatus.Offline,
)

@Composable
private fun userStatusText(status: UserChatStatus): String = when (status) {
    UserChatStatus.Online -> stringResource(R.string.online_status)
    UserChatStatus.Away -> stringResource(R.string.away_status)
    UserChatStatus.Busy -> stringResource(R.string.busy_status)
    UserChatStatus.Offline -> stringResource(R.string.offline_status)
    UserChatStatus.Invalid -> ""
}

@CombinedThemeComponentPreviews
@Composable
private fun ChatStatusDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatStatusDialogContent(
            currentStatus = UserChatStatus.Online,
            onOptionSelected = {},
            onDismissRequest = {},
        )
    }
}
