package mega.privacy.android.app.presentation.view.open.camera.confirmation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun OpenCameraConfirmationDialogRoute(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpenCameraConfirmationViewModel = hiltViewModel(),
) {
    if (viewModel.hasSuccessfullyDisableOngoingVideo) {
        onConfirm()
        viewModel.resetOngoingVideoDisablementState()
    }

    OpenCameraConfirmationDialog(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        onConfirm = viewModel::disableOngoingVideo,
        onDismiss = onDismiss
    )
}

@Composable
internal fun OpenCameraConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialog(
        modifier = modifier.testTag(OPEN_CAMERA_CONFIRMATION_DIALOG_TAG),
        title = stringResource(id = R.string.title_confirmation_open_camera_on_chat),
        text = stringResource(id = R.string.confirmation_open_camera_on_chat),
        confirmButtonText = stringResource(id = R.string.context_open_link),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@CombinedTextAndThemePreviews
@Composable
private fun OpenCameraConfirmationDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OpenCameraConfirmationDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

internal const val OPEN_CAMERA_CONFIRMATION_DIALOG_TAG =
    "open_camera_confirmation_dialog:dialog_open_camera_confirmation"
