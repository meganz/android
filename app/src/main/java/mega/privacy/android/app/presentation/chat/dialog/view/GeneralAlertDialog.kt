package mega.privacy.android.app.presentation.chat.dialog.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.presentation.theme.Typography
import mega.privacy.android.presentation.theme.dark_grey
import mega.privacy.android.presentation.theme.grey_alpha_060
import mega.privacy.android.presentation.theme.grey_alpha_087
import mega.privacy.android.presentation.theme.teal_200
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_060

/**
 * General alert dialog
 *
 * @param title                     Id of title text.
 * @param description               Id of description text.
 * @param confirmButton             Id of confirmButton text.
 * @param dismissButton             Id of dismissButton text.
 * @param onDismiss                 When dismiss the alert dialog.
 * @param onConfirmButton           When confirm the alert dialog.
 */
@Composable
fun GeneralAlertDialog(
    title: Int,
    description: Int,
    confirmButton: Int,
    dismissButton: Int,
    shouldDismissOnBackPress: Boolean,
    shouldDismissOnClickOutside: Boolean,
    onDismiss: () -> Unit,
    onConfirmButton: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = shouldDismissOnBackPress,
            dismissOnClickOutside = shouldDismissOnClickOutside
        ),
        confirmButton = {
            TextButton(onClick = { onConfirmButton() })
            {
                Text(
                    color = if (MaterialTheme.colors.isLight)
                        teal_300
                    else
                        teal_200,
                    text = stringResource(id = confirmButton))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() })
            {
                Text(
                    color = if (MaterialTheme.colors.isLight)
                        teal_300
                    else
                        teal_200,
                    text = stringResource(id = dismissButton))
            }
        },
        title = {
            Text(
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .padding(start = 0.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
                textAlign = TextAlign.Center,
                style = Typography.h6,
                color = if (MaterialTheme.colors.isLight)
                    grey_alpha_087
                else
                    white,
                fontWeight = FontWeight.Bold,
                text = stringResource(id = title))
        },
        text = {
            Text(
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .padding(start = 0.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
                text = stringResource(id = description),
                style = Typography.subtitle1,
                color = if (MaterialTheme.colors.isLight)
                    grey_alpha_060
                else
                    white_alpha_060)
        },
        backgroundColor = if (MaterialTheme.colors.isLight)
            white
        else
            dark_grey
    )
}