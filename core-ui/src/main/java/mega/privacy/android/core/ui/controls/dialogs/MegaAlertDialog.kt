package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.preview.PreviewAlertDialogParametersProvider
import mega.privacy.android.core.ui.controls.preview.PreviewStringParameters
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.utils.composeLet


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

@Composable
internal fun BaseMegaAlertDialog(
    text: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    text = {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            color = MegaTheme.colors.text.secondary
        )
    },
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    modifier = modifier,
    title = title,
    onCancel = onCancel,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BaseMegaAlertDialog(
    text: @Composable (() -> Unit),
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
    AlertDialog(
        modifier = modifier,
        backgroundColor = MegaTheme.colors.background.surface1,
        title = title?.composeLet {
            Text(
                modifier = Modifier
                    .testTag(TITLE_TAG)
                    .fillMaxWidth(),
                text = it,
                style = MaterialTheme.typography.h6,
                color = MegaTheme.colors.text.primary,
            )
        },
        text = text,
        onDismissRequest = onDismiss,
        buttons = {
            AlertDialogFlowRow {
                cancelButtonText?.let {
                    TextMegaButton(
                        modifier = Modifier.testTag(CANCEL_TAG),
                        text = cancelButtonText,
                        onClick = onCancel,
                    )
                }
                TextMegaButton(
                    modifier = Modifier.testTag(CONFIRM_TAG),
                    text = confirmButtonText,
                    onClick = onConfirm,
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    )
}

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreview(
    @PreviewParameter(PreviewAlertDialogParametersProvider::class) texts: PreviewStringParameters,
) {
    AndroidThemeForPreviews() {
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

internal const val TITLE_TAG = "titleTag"
internal const val CANCEL_TAG = "cancelTag"
internal const val CONFIRM_TAG = "confirmTag"

