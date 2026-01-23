package mega.privacy.android.feature.photos.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun EditVideoPlaylistDialog(
    handle: Long,
    title: String,
    positiveButtonText: String,
    onConfirm: (Long, String) -> Unit,
    resetErrorMessage: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    initialInputText: () -> String = { "" },
    inputPlaceHolderText: () -> String = { "" },
    errorText: String? = null,
) {
    var playlistTitle by rememberSaveable { mutableStateOf(title) }
    BasicInputDialog(
        modifier = modifier,
        title = title,
        positiveButtonText = positiveButtonText,
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        inputValue = initialInputText(),
        placeholder = inputPlaceHolderText(),
        onPositiveButtonClicked = {
            resetErrorMessage()
            onConfirm(handle, playlistTitle)
        },
        onNegativeButtonClicked = {
            resetErrorMessage()
            onDismiss()
        },
        onValueChange = {
            resetErrorMessage()
            playlistTitle = it
        },
        onDismiss = {
            resetErrorMessage()
            onDismiss()
        },
        errorText = errorText
    )
}