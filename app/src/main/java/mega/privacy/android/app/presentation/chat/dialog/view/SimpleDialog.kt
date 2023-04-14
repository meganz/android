package mega.privacy.android.app.presentation.chat.dialog.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.MegaDialog
import mega.privacy.android.core.ui.theme.grey_alpha_060
import mega.privacy.android.core.ui.theme.subtitle1
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white_alpha_060

/**
 * Simple dialog
 *
 * @param title                     Id of title text.
 * @param description               Id of description text.
 * @param confirmButton             Id of confirmButton text.
 * @param dismissButton             Id of dismissButton text.
 * @param onDismiss                 When dismiss the alert dialog.
 * @param onConfirmButton           When confirm the alert dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimpleDialog(
    title: Int?,
    description: Int,
    confirmButton: Int,
    dismissButton: Int,
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
        onDismissRequest = { onDismiss() },

        titleString = if (title != null) stringResource(id = title) else null,
        titleAlign = TextAlign.Start,
        body = {
            Text(
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth(),
                text = stringResource(id = description),
                textAlign = TextAlign.Start,
                style = subtitle1,
                color = if (MaterialTheme.colors.isLight)
                    grey_alpha_060
                else
                    white_alpha_060
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirmButton() },
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
            ) {
                Text(
                    text = stringResource(id = confirmButton),
                    color = if (MaterialTheme.colors.isLight)
                        teal_300
                    else
                        teal_200
                )
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
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
            ) {
                Text(
                    stringResource(id = dismissButton),
                    color = if (MaterialTheme.colors.isLight)
                        teal_300
                    else
                        teal_200,
                )
            }
        }
    )
}