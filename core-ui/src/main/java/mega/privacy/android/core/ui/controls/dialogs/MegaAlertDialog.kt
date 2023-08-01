package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.utils.composeLet


/**
 * Alert dialog with a text an a confirmation button with optional title and cancel button
 * Confirm and cancel button will be placed horizontally if there are enough room, vertically if not.
 * @param text main text to be shown
 * @param confirmButtonText text for the confirm button
 * @param cancelButtonText text for the cancel button, if null there will be no cancel button
 * @param onConfirm to be triggered when confirm button is pressed
 * @param onDismiss to be triggered when dialog is hidden, wither with cancel button, confirm button, back or outside press.
 * @param title the title of the dialog, if no there will be no title
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MegaAlertDialog(
    text: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
    AlertDialog(
        modifier = modifier,
        title = title?.composeLet {
            Text(
                modifier = Modifier
                    .testTag(TITLE_TAG)
                    .fillMaxWidth(),
                text = it,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorSecondary
            )
        },
        onDismissRequest = onDismiss,
        buttons = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextMegaButton(
                    modifier = Modifier.testTag(CONFIRM_TAG),
                    text = confirmButtonText,
                    onClick = onConfirm,
                )
                cancelButtonText?.let {
                    TextMegaButton(
                        modifier = Modifier.testTag(CANCEL_TAG),
                        text = cancelButtonText,
                        onClick = onDismiss,
                    )
                }
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
private fun MegaAlertDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                text = "Discard draft?",
                confirmButtonText = "Discard",
                cancelButtonText = "Cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreviewLongAction() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                text = "Discard draft?",
                confirmButtonText = "Very long long long action",
                cancelButtonText = "Very long Cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreviewTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                title = "Dialog title",
                text = "Discard draft draft draft draft draft draft draft draft draft draft ",
                confirmButtonText = "Discard",
                cancelButtonText = "Cancel",
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

