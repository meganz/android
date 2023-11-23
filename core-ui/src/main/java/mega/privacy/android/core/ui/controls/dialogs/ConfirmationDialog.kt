package mega.privacy.android.core.ui.controls.dialogs


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.core.ui.controls.dialogs.internal.ButtonsColumn
import mega.privacy.android.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.OPTION1_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.OPTION2_TAG
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

@Composable
internal fun ConfirmationDialog(
    title: String,
    text: String,
    buttonOption1Text: String,
    buttonOption2Text: String,
    cancelButtonText: String,
    onOption1: () -> Unit,
    onOption2: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    text = text,
    buttons = {
        ButtonsColumn {
            TextMegaButton(
                modifier = Modifier.testTag(OPTION1_TAG),
                text = buttonOption1Text,
                onClick = onOption1,
            )
            TextMegaButton(
                modifier = Modifier.testTag(OPTION2_TAG),
                text = buttonOption2Text,
                onClick = onOption2,
            )
            TextMegaButton(
                modifier = Modifier.testTag(CANCEL_TAG),
                text = cancelButtonText,
                onClick = onCancel,
            )
        }
    },
    onDismiss, modifier, title, dismissOnClickOutside, dismissOnBackPress
)

@CombinedThemePreviews
@Composable
private fun ConfirmationDialogPreview() {
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
private fun ConfirmationDialogRtlPreview(
    @PreviewParameter(PreviewStringsParametersProviderWithTitle::class) texts: PreviewStringParameters,
) {
    texts.title?.let {
        AndroidThemeForPreviews {
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

@CombinedThemePreviews
@Composable
private fun ConfirmationDialog3ButtonsPreview() {
    AndroidThemeForPreviews {
        ConfirmationDialog(
            title = "Dialog title",
            text = "This is the message body of the dialog. And this is another line in the test.",
            buttonOption1Text = "Action 1",
            buttonOption2Text = "Action 2",
            cancelButtonText = "Cancel",
            onOption1 = {},
            onOption2 = {},
            onDismiss = {},
        )
    }
}


internal class PreviewStringsParametersProviderWithTitle : PreviewAlertDialogParametersProvider() {
    override val values: Sequence<PreviewStringParameters>
        get() = super.values.filter { it.title != null }
}
