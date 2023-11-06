package mega.privacy.android.core.ui.controls.dialogs


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

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

@CombinedThemePreviews
@Composable
private fun PreviewConfirmationDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialog(
            title = "Dialog title",
            text = "This is the message body of the text. And this is another line in the test. ",
            cancelButtonText = "Cancel",
            confirmButtonText = "Ok",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
