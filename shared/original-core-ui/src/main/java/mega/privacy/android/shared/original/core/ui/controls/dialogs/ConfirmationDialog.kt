package mega.privacy.android.shared.original.core.ui.controls.dialogs


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.ButtonsColumn
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.OPTION1_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.OPTION2_TAG
import mega.privacy.android.shared.original.core.ui.controls.preview.PreviewAlertDialogParametersProvider
import mega.privacy.android.shared.original.core.ui.controls.preview.PreviewStringParameters
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempThemeForPreviews

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
 * Confirmation dialog with only a message and 2 buttons.
 * The 2 buttons have short text and are in a horizontal row.
 *
 * @param text message body
 * @param cancelButtonText cancel button text
 * @param confirmButtonText confirmation button text
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param onConfirm to be triggered when confirmation button is pressed
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 */
@Composable
fun ConfirmationDialog(
    text: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    modifier = modifier,
    text = text,
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    onCancel = onDismiss,
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
)

/**
 * Confirmation dialog with only title and 2 buttons.
 * The 2 buttons have short text and are in a horizontal row.
 *
 * @param title title
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
    text = null,
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
    content = text,
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
 * Confirmation dialog with a title, a message body and 3 buttons.
 * The 3 buttons have short text and are in a vertical column.
 *
 * @param title title
 * @param text message body
 * @param cancelButtonText cancel button text
 * @param buttonOption1Text option 1 button text
 * @param buttonOption2Text option 2 button text
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param onOption1 to be triggered when option 1 button is pressed
 * @param onOption2 to be triggered when option 2 button is pressed
 * @param onCancel  to be triggered when cancel button is clicked
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 */
@Composable
fun ConfirmationDialog(
    title: String?,
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
            DialogEndButton(
                modifier = Modifier.testTag(OPTION1_TAG),
                text = buttonOption1Text,
                onClick = onOption1,
            )
            DialogEndButton(
                modifier = Modifier.testTag(OPTION2_TAG),
                text = buttonOption2Text,
                onClick = onOption2,
            )
            DialogEndButton(
                modifier = Modifier.testTag(CANCEL_TAG),
                text = cancelButtonText,
                onClick = onCancel,
            )
        }
    },
    onDismiss, modifier, title, dismissOnClickOutside, dismissOnBackPress
)

/**
 * Button with end text alignment, in case it takes more than one line
 */
@Composable
private fun DialogEndButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = TextMegaButton(text, onClick, modifier, textAlign = TextAlign.End)

@CombinedThemePreviews
@Composable
private fun ConfirmationDialogPreview() {
    OriginalTempThemeForPreviews {
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

@CombinedThemePreviews
@Composable
private fun ConfirmationDialogOnlyTitlePreview() {
    OriginalTempThemeForPreviews {
        ConfirmationDialog(
            title = "Dialog title",
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
        OriginalTempThemeForPreviews {
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
    OriginalTempThemeForPreviews {
        ConfirmationDialog(
            title = "Dialog title",
            text = "This is the message body of the dialog. And this is another line in the test.",
            buttonOption1Text = "Action 1",
            buttonOption2Text = "Action 2 with a very long title that takes more than one line",
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
