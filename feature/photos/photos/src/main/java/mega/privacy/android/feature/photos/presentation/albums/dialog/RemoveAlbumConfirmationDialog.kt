package mega.privacy.android.feature.photos.presentation.albums.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun RemoveAlbumConfirmationDialog(
    size: Int,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier,
        title = pluralStringResource(
            id = sharedR.plurals.delete_album_confirmation_dialog_title,
            count = size
        ),
        description = pluralStringResource(
            id = sharedR.plurals.delete_album_confirmation_dialog_description,
            count = size
        ),
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