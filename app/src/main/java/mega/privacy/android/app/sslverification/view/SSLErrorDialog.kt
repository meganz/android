package mega.privacy.android.app.sslverification.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

private const val SSL_HELP_URL = "https://help.mega.io/security/data-protection/resolve-ssl-errors"

@Composable
internal fun SSLErrorDialog(
    closeDialog: () -> Unit,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    BasicDialog(
        modifier = Modifier,
        title = SpannableText(stringResource(id = sharedR.string.ssl_error_dialog_secure_connection_failed_title)),
        description = SpannableText(
            text = stringResource(id = sharedR.string.ssl_error_dialog_secure_connection_failed_body),
            annotations = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        LinkColor.Primary
                    ),
                    annotation = "A"
                )
            ),
            onAnnotationClick = {
                context.launchUrl(SSL_HELP_URL)
            }
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
        SSLErrorDialog(
            closeDialog = { },
            onRetry = { },
            onOpenBrowser = { },
            onDismiss = { }
        )
    }
}