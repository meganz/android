package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.PasswordInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Open password link dialog
 */
@Composable
fun OpenPasswordLinkDialog(
    passwordProtectedLink: String,
    onDismiss: () -> Unit,
    onNavigateToFileLink: (String) -> Unit,
    onNavigateToFolderLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpenPasswordLinkViewModel = hiltViewModel(),
) {
    var password by remember { mutableStateOf("") }
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        viewState.decryptedLinkEvent,
        onConsumed = viewModel::consumeDecryptedLinkEvent,
    ) {
        when (it) {
            is DecryptedLink.FileLink -> onNavigateToFileLink(it.link)
            is DecryptedLink.FolderLink -> onNavigateToFolderLink(it.link)
        }
    }
    OpenPasswordLinkDialog(
        password = password,
        errorText = if (viewState.errorMessage) stringResource(sharedR.string.password_dialog_error) else null,
        onValueChanged = {
            viewModel.resetError()
            password = it
        },
        onConfirm = {
            viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)
        },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
internal fun OpenPasswordLinkDialog(
    password: String,
    errorText: String?,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasswordInputDialog(
        modifier = modifier.testTag(OPEN_PASSWORD_TAG),
        title = stringResource(sharedR.string.password_dialog_hint),
        positiveButtonText = stringResource(sharedR.string.general_decrypt),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        errorText = errorText,
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        inputValue = password,
        onValueChange = onValueChanged
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewCannotVerifyContactDialog() {
    AndroidThemeForPreviews {
        OpenPasswordLinkDialog("", {}, {}, {})
    }
}

internal const val OPEN_PASSWORD_TAG = "open_password_dialog"