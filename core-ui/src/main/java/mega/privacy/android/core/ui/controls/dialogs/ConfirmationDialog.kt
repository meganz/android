package mega.privacy.android.core.ui.controls.dialogs


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.core.ui.controls.preview.PreviewAlertDialogParametersProvider
import mega.privacy.android.core.ui.controls.preview.PreviewStringParameters
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Confirmation dialog with a title, a message body and 2 buttons.
 * The 2 buttons have short text and are in a horizontal row.
 *
 * @param title title
 * @param text message body
 * @param cancelButtonText cancel button text
 * @param confirmButtonText confirmation button text
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param onConfirm to be triggered when confirmation button is pressed
 * @param onCancel  to be triggered when cancel button is clicked
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    modifier = modifier,
    text = text,
    title = title,
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    onCancel = onCancel,
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
)

/**
 * Confirmation dialog with a title, a message body and 2 buttons.
 * The 2 buttons have short text and are in a horizontal row.
 *
 * @param title title
 * @param text message body
 * @param cancelButtonText cancel button text
 * @param confirmButtonText confirmation button text
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param onConfirm to be triggered when confirmation button is pressed
 * @param onCancel  to be triggered when cancel button is clicked
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: @Composable (() -> Unit),
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    modifier = modifier,
    text = text,
    title = title,
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    onCancel = onCancel,
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
)

@CombinedThemePreviews
@Composable
private fun PreviewConfirmationDialog() {
    AndroidThemeForPreviews {
        ConfirmationDialog(
            title = "Dialog title",
            text = "This is the message body of the dialog. And this is another line in the test.",
            cancelButtonText = "Cancel",
            confirmButtonText = "Ok",
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun BaseMegaAlertDialogPreview(
    @PreviewParameter(PreviewStringsParametersProviderWithTitle::class) texts: PreviewStringParameters,
) {
    texts.title?.let {
        AndroidThemeForPreviews() {
            ConfirmationDialog(
                title = texts.title.getText(),
                text = texts.text.getText(),
                confirmButtonText = texts.confirmButtonText.getText(),
                cancelButtonText = texts.cancelButtonText?.getText(),
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}


internal class PreviewStringsParametersProviderWithTitle : PreviewAlertDialogParametersProvider() {
    override val values: Sequence<PreviewStringParameters>
        get() = super.values.filter { it.title != null }
}
