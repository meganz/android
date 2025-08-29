package mega.privacy.android.core.nodecomponents.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Dialog to show when a node is taken down
 * @param isFolder If node is folder
 * @param onDismiss Dismiss callback for the dialog
 */
@Composable
fun TakeDownDialog(
    isFolder: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val megaNavigator = rememberMegaNavigator()
    BasicDialog(
        title = SpannableText(
            stringResource(
                id = if (isFolder) {
                    R.string.dialog_taken_down_folder_title
                } else {
                    R.string.dialog_taken_down_file_title
                }
            )
        ),
        description = SpannableText(
            text = stringResource(
                if (isFolder) {
                    R.string.dialog_taken_down_folder_description
                } else {
                    R.string.dialog_taken_down_file_description
                }
            ),
            annotations = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.DefaultColorStyle(
                        spanStyle = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = DSTokens.colors.text.accent
                        ),
                    ),
                    annotation = "A"
                )
            ),
            onAnnotationClick = {
                megaNavigator.openTakedownPolicyLink(context)
            }
        ),
        positiveButtonText = stringResource(sharedResR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        negativeButtonText = stringResource(R.string.dispute_takendown_file),
        onNegativeButtonClicked = {
            megaNavigator.openDisputeTakedownLink(context)
            onDismiss()
        }
    )
}

/**
 * PreviewTakeDownDialogWhenFolderTrue
 */
@CombinedThemePreviews
@Composable
fun PreviewTakeDownDialogWhenFolderTrue() {
    AndroidThemeForPreviews {
        TakeDownDialog(
            isFolder = true,
            onDismiss = {},
        )
    }
}

/**
 * PreviewTakeDownDialogWhenFolderFalse
 */
@CombinedThemePreviews
@Composable
fun PreviewTakeDownDialogWhenFolderFalse() {
    AndroidThemeForPreviews {
        TakeDownDialog(
            isFolder = false,
            onDismiss = {},
        )
    }
}