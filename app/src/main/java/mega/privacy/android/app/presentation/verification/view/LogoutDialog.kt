package mega.privacy.android.app.presentation.verification.view

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.MegaDialog

/**
 * Logout Dialog
 *
 * @param title                         Id of title text.
 * @param confirmButtonLabel            confirmButton text.
 * @param dismissButtonLabel            dismissButton text.
 * @param shouldDismissOnBackPress      whether dismiss on back press or not
 * @param shouldDismissOnClickOutside   whether dismiss on outside click or not
 * @param onDismiss                     When dismiss the alert dialog.
 * @param onConfirmButton               When confirm the alert dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LogoutDialog(
    title: String,
    confirmButtonLabel: String,
    dismissButtonLabel: String,
    shouldDismissOnBackPress: Boolean,
    shouldDismissOnClickOutside: Boolean,
    onDismiss: () -> Unit,
    onConfirmButton: () -> Unit,
) {
    MegaDialog(
        modifier = Modifier
            .padding(horizontal = 40.dp)
            .widthIn(max = 280.dp),
        properties = DialogProperties(
            dismissOnBackPress = shouldDismissOnBackPress,
            dismissOnClickOutside = shouldDismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = onDismiss,
        titleString = title,
        titleAlign = TextAlign.Start,
        confirmButton = {
            LogoutConfirmationButton(onClick = onConfirmButton) {
                Text(
                    text = confirmButtonLabel,
                    color = MaterialTheme.colors.secondary,
                )
            }
        },
        dismissButton = {
            LogoutConfirmationButton(onClick = onDismiss) {
                Text(
                    dismissButtonLabel,
                    color = MaterialTheme.colors.secondary,
                )
            }
        }
    )
}

@Composable
private fun LogoutConfirmationButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
        ),
        modifier = Modifier.padding(all = 0.dp),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp
        ),
        content = content
    )
}
