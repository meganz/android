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
fun AddNewAlbumDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var albumName by rememberSaveable { mutableStateOf("") }

    BasicInputDialog(
        modifier = modifier,
        title = stringResource(sharedResR.string.media_add_new_album_dialog_title),
        positiveButtonText = stringResource(sharedResR.string.media_add_new_album_dialog_positive_button),
        negativeButtonText = stringResource(sharedResR.string.general_dialog_cancel_button),
        inputValue = albumName,
        onPositiveButtonClicked = {
            onConfirm(albumName)
            onDismiss()
        },
        onNegativeButtonClicked = {
            onDismiss()
            albumName = ""
        },
        onValueChange = {
            albumName = it
        },
        onDismiss = {
            onDismiss()
            albumName = ""
        }
    )
}

@CombinedThemePreviews
@Composable
fun AddNewAlbumDialogPreview() {
    AndroidThemeForPreviews {
        AddNewAlbumDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}