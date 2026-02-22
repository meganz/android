package mega.privacy.android.app.sslverification.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.linkText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

private const val SSL_HELP_URL = "https://help.mega.io/security/data-protection/resolve-ssl-errors"

@Composable
internal fun SSLErrorDialog(
    closeDialog: () -> Unit,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
    launchUrl: (String) -> Unit,
) {
    BasicDialog(
        modifier = Modifier,
        title = SpannableText(stringResource(id = sharedR.string.ssl_error_dialog_secure_connection_failed_title)),
        description = linkText(
            stringResourceIdentifier = sharedR.string.ssl_error_dialog_secure_connection_failed_body,
            onClick = launchUrl,
            url = SSL_HELP_URL,
        ),
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
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
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
        Column(modifier = Modifier.fillMaxHeight()) {
            SSLErrorDialog(
                closeDialog = { },
                onRetry = { },
                onOpenBrowser = { },
                onDismiss = {},
                launchUrl = {}
            )
        }
    }
}