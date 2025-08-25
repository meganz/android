package mega.privacy.android.app.sslverification.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

@Composable
internal fun SSLErrorDialog(
    closeDialog: () -> Unit,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier,
        title = stringResource(id = R.string.ssl_error_dialog_title),
        description = stringResource(id = R.string.ssl_error_dialog_text),
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = R.string.general_retry),
                onClick = {
                    onRetry()
                    closeDialog()
                }
            ),
            BasicDialogButton(
                text = stringResource(id = R.string.general_open_browser),
                onClick = {
                    onOpenBrowser()
                }
            ),
            BasicDialogButton(
                text = stringResource(id = R.string.general_dismiss),
                onClick = {
                    onDismiss()
                    closeDialog()
                }
            )
        ),
        onDismissRequest = {
            onDismiss()
            closeDialog()
        },
        buttonDirection = VERTICAL
    )
}


@CombinedThemePreviews
@Composable
private fun SSLErrorDialogPreview() {
    AndroidThemeForPreviews {
        SSLErrorDialog(
            closeDialog = { },
            onRetry = { },
            onOpenBrowser = { },
            onDismiss = { }
        )
    }
}