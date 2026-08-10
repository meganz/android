package mega.privacy.android.feature.photos.presentation.albums.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContentTriggered
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.navigation.destination.CreateAlbumDialogNavKey
import mega.privacy.android.navigation.destination.CreateAlbumDialogResult
import mega.privacy.android.shared.resources.R as sharedResR

internal const val CREATE_ALBUM_DIALOG_TAG = "create_album_dialog:input_dialog"

/**
 * Dialog for creating a new user album. It owns its own [CreateAlbumDialogViewModel] and, once the
 * album is created, returns the result to the launching screen via [returnResult] so each caller
 * can decide how to follow up (e.g. open photo selection or the new album).
 *
 * @param onDismiss dismisses the dialog route.
 * @param returnResult publishes the created album under [CreateAlbumDialogNavKey.RESULT].
 */
@Composable
fun CreateAlbumDialogM3(
    onDismiss: () -> Unit,
    returnResult: (String, CreateAlbumDialogResult?) -> Unit,
    viewModel: CreateAlbumDialogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        event = state.albumCreatedEvent,
        onConsumed = viewModel::resetAlbumCreatedEvent,
    ) { result ->
        returnResult(CreateAlbumDialogNavKey.RESULT, result)
        onDismiss()
    }

    CreateAlbumDialogBody(
        placeholder = state.placeholder,
        errorText = (state.errorMessage as? StateEventWithContentTriggered)?.content,
        onConfirm = viewModel::createAlbum,
        resetErrorMessage = viewModel::resetErrorMessage,
        onDismiss = onDismiss,
    )
}

@Composable
private fun CreateAlbumDialogBody(
    placeholder: String,
    errorText: String?,
    onConfirm: (String) -> Unit,
    resetErrorMessage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var albumName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    BasicInputDialog(
        modifier = modifier.testTag(CREATE_ALBUM_DIALOG_TAG),
        title = stringResource(sharedResR.string.media_add_new_album_dialog_title),
        positiveButtonText = stringResource(sharedResR.string.general_create_label),
        negativeButtonText = stringResource(sharedResR.string.general_dialog_cancel_button),
        inputValue = albumName,
        onPositiveButtonClicked = {
            resetErrorMessage()
            onConfirm(albumName.text)
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
        errorText = errorText,
        placeholder = placeholder,
        capitalization = KeyboardCapitalization.Sentences,
    )
}

@CombinedThemePreviews
@Composable
private fun CreateAlbumDialogBodyPreview() {
    AndroidThemeForPreviews {
        CreateAlbumDialogBody(
            placeholder = "New album",
            errorText = null,
            onConfirm = {},
            resetErrorMessage = {},
            onDismiss = {},
        )
    }
}
