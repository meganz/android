package mega.privacy.android.feature.photos.presentation.albums.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun EnterAlbumNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    resetErrorMessage: () -> Unit,
    positiveButtonText: String,
    modifier: Modifier = Modifier,
    name: String = "",
    errorText: String? = null,
) {
    var albumName by rememberSaveable { mutableStateOf(name) }

    BasicInputDialog(
        modifier = modifier,
        title = stringResource(sharedResR.string.media_add_new_album_dialog_title),
        positiveButtonText = positiveButtonText,
        negativeButtonText = stringResource(sharedResR.string.general_dialog_cancel_button),
        inputValue = albumName,
        onPositiveButtonClicked = {
            resetErrorMessage()
            onConfirm(albumName)
        },
        onNegativeButtonClicked = {
            resetErrorMessage()
            onDismiss()
        },
        onValueChange = {
            resetErrorMessage()
            albumName = it
        },
        onDismiss = {
            resetErrorMessage()
            onDismiss()
        },
        errorText = errorText
    )
}

@CombinedThemePreviews
@Composable
private fun AddNewAlbumDialogPreview() {
    AndroidThemeForPreviews {
        EnterAlbumNameDialog(
            onConfirm = {},
            onDismiss = {},
            resetErrorMessage = {},
            positiveButtonText = ""
        )
    }
}