package mega.privacy.android.app.presentation.chat.dialog.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.grey_alpha_060
import mega.privacy.android.presentation.theme.grey_alpha_087
import mega.privacy.android.presentation.theme.h6
import mega.privacy.android.presentation.theme.subtitle1
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GeneralAlertDialog(
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .padding(horizontal = 40.dp),
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = shouldDismissOnBackPress,
            dismissOnClickOutside = shouldDismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
        confirmButton = {
            TextButton(onClick = { onConfirmButton() })
            {
                Text(
                    modifier = modifier.padding(bottom = 18.dp),
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
                    modifier = modifier.padding(bottom = 18.dp),
                    color = if (MaterialTheme.colors.isLight)
                        teal_300
                    else
                        teal_200,
                    text = stringResource(id = dismissButton))
            }
        },
        title = {
            Text(
                modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
                textAlign = TextAlign.Start,
                style = h6,
                color = if (MaterialTheme.colors.isLight)
                    grey_alpha_087
                else
                    white,
                text = stringResource(id = title))
        },
        text = {
            Text(
                modifier = modifier
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                text = stringResource(id = description),
                textAlign = TextAlign.Start,
                style = subtitle1,
                color = if (MaterialTheme.colors.isLight)
                    grey_alpha_060
                else
                    white_alpha_060)
        },
        backgroundColor = if (MaterialTheme.colors.isLight)
            white
        else
            colorResource(id = R.color.action_mode_background),
    )
}