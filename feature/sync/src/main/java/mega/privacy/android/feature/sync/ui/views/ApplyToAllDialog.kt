package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.body2
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog that allows users to choose whether to apply a resolution action
 * to the current conflict only or to all similar conflicts.
 *
 * @param title The dialog title (e.g., "Choose the local file?")
 * @param description The description explaining what will happen
 * @param onApplyToCurrent Callback when user chooses to apply to current item only
 * @param onApplyToAll Callback when user chooses to apply to all similar conflicts
 * @param onCancel Callback when user cancels the dialog
 * @param modifier Modifier for the dialog
 */
@Composable
internal fun ApplyToAllDialog(
    title: String,
    description: String,
    onApplyToCurrent: () -> Unit,
    onApplyToAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isApplyToAllChecked by rememberSaveable { mutableStateOf(false) }

    ConfirmationDialog(
        title = title,
        text = {
            Column {
                // Description
                MegaSpannedText(
                    value = description,
                    baseStyle = body2,
                    styles = hashMapOf(
                        SpanIndicator('A') to MegaSpanStyle(
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
                            color = TextColor.Primary,
                        ),
                    ),
                    color = TextColor.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_DESCRIPTION),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Apply to all checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MegaCheckbox(
                        modifier = Modifier
                            .size(32.dp)
                            .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX),
                        checked = isApplyToAllChecked,
                        onCheckedChange = { isApplyToAllChecked = it },
                        rounded = false
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    MegaText(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT),
                        text = stringResource(sharedR.string.sync_apply_to_all_checkbox),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle1,
                    )
                }
            }
        },
        confirmButtonText = stringResource(sharedR.string.general_dialog_choose_button),
        cancelButtonText = stringResource(mega.privacy.android.core.R.string.general_cancel),
        onConfirm = {
            if (isApplyToAllChecked) {
                onApplyToAll()
            } else {
                onApplyToCurrent()
            }
        },
        onDismiss = onCancel,
        onCancel = onCancel,
        modifier = modifier,
    )
}

// Test tags for UI testing
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_DESCRIPTION = "apply_to_all_dialog:description"
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW = "apply_to_all_dialog:checkbox_row"
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX = "apply_to_all_dialog:checkbox"
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT = "apply_to_all_dialog:checkbox_text"

@CombinedThemePreviews
@Composable
internal fun ApplyToAllDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ApplyToAllDialog(
            title = "Choose the local file?",
            description = "The local file Let only red flowers bloom.pdf will be moved to the .rubbish or .debris folder in your local sync location.",
            onApplyToCurrent = {},
            onApplyToAll = {},
            onCancel = {},
        )
    }
} 
