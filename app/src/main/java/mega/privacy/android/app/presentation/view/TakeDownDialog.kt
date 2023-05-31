package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.MegaSpannedClickableText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.model.SpanStyleWithAnnotation
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * This function shows TakeDown Dialog when clicked on node which is taken down
 * @param isFolder Node is folder
 * @param onConfirm
 * @param onDeny
 * @param onLinkClick
 */
@Composable
fun TakeDownDialog(
    isFolder: Boolean,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
    onLinkClick: (String) -> Unit
) {

    AlertDialog(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        onDismissRequest = onDeny,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                stringResource(
                    id =
                    if (isFolder) R.string.dialog_taken_down_folder_title else R.string.dialog_taken_down_file_title
                )
            )
        },
        text = {
            MegaSpannedClickableText(
                value = stringResource(
                    id =
                    if (isFolder) R.string.dialog_taken_down_folder_description else R.string.dialog_taken_down_file_description
                ),
                styles = mapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colors.teal_300_teal_200
                        ),
                        Constants.TAKEDOWN_URL
                    )
                ),
                onAnnotationClick = onLinkClick,
                baseStyle = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorSecondary)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.general_ok),
                    color = MaterialTheme.colors.teal_300_teal_200
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(
                    text = stringResource(R.string.dispute_takendown_file),
                    color = MaterialTheme.colors.teal_300_teal_200,
                )
            }
        },
    )
}

/**
 * PreviewTakeDownDialogWhenFolderTrue
 */
@CombinedTextAndThemePreviews
@Composable
fun PreviewTakeDownDialogWhenFolderTrue() {
    TakeDownDialog(
        isFolder = true,
        onConfirm = {},
        onDeny = {},
        onLinkClick = {}
    )
}

/**
 * PreviewTakeDownDialogWhenFolderFalse
 */
@CombinedTextAndThemePreviews
@Composable
fun PreviewTakeDownDialogWhenFolderFalse() {
    TakeDownDialog(
        isFolder = false,
        onConfirm = {},
        onDeny = {},
        onLinkClick = {}
    )
}