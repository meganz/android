package mega.privacy.android.feature.clouddrive.presentation.folderlink.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.PasswordInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun DecryptionKeyDialog(
    isKeyIncorrect: Boolean,
    onDecryptionKeyEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var key by rememberSaveable { mutableStateOf("") }

    PasswordInputDialog(
        modifier = modifier.testTag(DECRYPTION_KEY_DIALOG_TAG),
        title = stringResource(sharedR.string.link_decryption_key_dialog_title),
        description = stringResource(sharedR.string.link_decryption_key_dialog_message),
        positiveButtonText = stringResource(sharedR.string.general_decrypt),
        onPositiveButtonClicked = { onDecryptionKeyEntered(key) },
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        inputValue = key,
        onValueChange = { key = it },
        errorText = if (isKeyIncorrect) {
            stringResource(sharedR.string.link_decryption_key_dialog_error_invalid_key)
        } else {
            null
        },
    )
}

@CombinedThemePreviews
@Composable
private fun DecryptionKeyDialogPreview() {
    AndroidThemeForPreviews {
        DecryptionKeyDialog(
            isKeyIncorrect = false,
            onDecryptionKeyEntered = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DecryptionKeyDialogErrorPreview() {
    AndroidThemeForPreviews {
        DecryptionKeyDialog(
            isKeyIncorrect = true,
            onDecryptionKeyEntered = {},
            onDismiss = {},
        )
    }
}

internal const val DECRYPTION_KEY_DIALOG_TAG = "folder_link_screen:decryption_key_dialog"
