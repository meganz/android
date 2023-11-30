package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.core.ui.controls.preview.PreviewAlertDialogParametersProvider
import mega.privacy.android.core.ui.controls.preview.PreviewStringParameters
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme


/**
 * Alert dialog with a text an a confirmation button with optional cancel button
 * Confirm and cancel button will be placed horizontally if there are enough room, vertically if not.
 *
 * @param text main text to be shown
 * @param confirmButtonText text for the confirm button
 * @param cancelButtonText text for the cancel button, if null there will be no cancel button
 * @param onConfirm to be triggered when confirm button is pressed
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 */

@Composable
fun MegaAlertDialog(
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
    text = text,
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    modifier = modifier,
    onCancel = onCancel,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress,
)

/**
 * Alert dialog with a text an a confirmation button with optional cancel button
 * Confirm and cancel button will be placed horizontally if there are enough room, vertically if not.
 *
 * @param text main text to be shown
 * @param confirmButtonText text for the confirm button
 * @param cancelButtonText text for the cancel button, if null there will be no cancel button
 * @param onConfirm to be triggered when confirm button is pressed
 * @param onDismiss to be triggered when dialog is hidden, whether with cancel button, confirm button, back or outside press.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 */
@Composable
fun MegaAlertDialog(
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
    text = text,
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    modifier = modifier,
    onCancel = onCancel,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress,
)

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreview(
    @PreviewParameter(PreviewAlertDialogParametersProvider::class) texts: PreviewStringParameters,
) {
    AndroidTheme {
        PreviewBox {
            MegaAlertDialog(
                text = texts.text.getText(),
                confirmButtonText = texts.confirmButtonText.getText(),
                cancelButtonText = texts.cancelButtonText?.getText(),
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}


@Composable
private fun PreviewBox(content: @Composable BoxScope.() -> Unit) = Box(
    modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
    content = content
)

