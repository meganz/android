package mega.privacy.android.feature.photos.presentation.albums.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DeleteAlbumsConfirmationDialogEvent

@Composable
internal fun RemoveAlbumConfirmationDialog(
    size: Int,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(DeleteAlbumsConfirmationDialogEvent)
    }

    BasicDialog(
        modifier = modifier,
        title = if (size == 1) {
            stringResource(sharedR.string.delete_album_singular_confirmation_dialog_title)
        } else {
            stringResource(sharedR.string.delete_albums_multiple_confirmation_dialog_title,)
        },
        description = if (size == 1) {
            stringResource(sharedR.string.delete_album_singular_confirmation_dialog_description,)
        } else {
            stringResource(sharedR.string.delete_albums_multiple_confirmation_dialog_description,)
        },
        positiveButtonText = stringResource(sharedR.string.delete_album_confirmation_positive_button_text),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        isVisible = isVisible
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveAlbumConfirmationDialogPreview() {
    AndroidThemeForPreviews {
        RemoveAlbumConfirmationDialog(
            size = 1,
            isVisible = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}