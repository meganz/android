package mega.privacy.android.core.ui.controls.dialogs.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.utils.composeLet

@Composable
internal fun BaseMegaAlertDialog(
    text: String?,
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
    content = text?.composeLet {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.subtitle1,
            color = MegaTheme.colors.text.secondary,
            modifier = Modifier.testTag(CONTENT_TAG),
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

@Composable
internal fun BaseMegaAlertDialog(
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
    title: String? = null,
    onCancel: () -> Unit = onDismiss,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
) = BaseMegaAlertDialog(
    text = content,
    buttons = {
        AlertDialogFlowRow {
            cancelButtonText?.let {
                TextMegaButton(
                    modifier = Modifier.testTag(CANCEL_TAG),
                    text = cancelButtonText,
                    onClick = onCancel,
                    enabled = cancelEnabled
                )
            }
            TextMegaButton(
                modifier = Modifier.testTag(CONFIRM_TAG),
                text = confirmButtonText,
                onClick = onConfirm,
                enabled = confirmEnabled
            )
        }
    },
    onDismiss = onDismiss,
    modifier = modifier,
    title = title,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress
)

@Composable
internal fun BaseMegaAlertDialog(
    text: String,
    buttons: @Composable (() -> Unit),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = BaseMegaAlertDialog(
    text = {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.subtitle1,
            color = MegaTheme.colors.text.secondary,
            modifier = Modifier.testTag(CONTENT_TAG),
        )
    },
    buttons = buttons,
    onDismiss = onDismiss,
    modifier = modifier,
    title = title,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BaseMegaAlertDialog(
    buttons: @Composable (() -> Unit),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    title: String? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) = CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
    AlertDialog(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        backgroundColor = MegaTheme.colors.background.surface1,
        title = title?.composeLet {
            Text(
                modifier = Modifier
                    .testTag(TITLE_TAG)
                    .conditional(text == null) {
                        // For dialog without text, add padding to the bottom of the title
                        padding(bottom = 18.dp)
                    }
                    .fillMaxWidth(),
                text = it,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.h6,
                color = MegaTheme.colors.text.primary,
            )
        },
        text = text,
        onDismissRequest = onDismiss,
        buttons = buttons,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    )
}

internal const val TITLE_TAG = "mega_alert_dialog:text_title"
internal const val CONTENT_TAG = "mega_alert_dialog:text_content"
internal const val CANCEL_TAG = "mega_alert_dialog:button_cancel"
internal const val CONFIRM_TAG = "mega_alert_dialog:button_confirm"
internal const val OPTION1_TAG = "mega_alert_dialog:button_option1"
internal const val OPTION2_TAG = "mega_alert_dialog:button_option2"