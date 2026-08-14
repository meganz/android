package mega.privacy.android.feature.sync.ui.views

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog that allows users to choose whether to apply a resolution action
 * to the current conflict only or to all similar conflicts.
 *
 * @param fileName The name of the file involved in the stalled issue
 * @param selectedAction The selected resolution action for the stalled issue
 * @param onApplyToCurrent Callback when user chooses to apply to current item only
 * @param onApplyToAll Callback when user chooses to apply to all similar conflicts
 * @param onCancel Callback when user cancels the dialog
 * @param modifier Modifier for the dialog
 */
@Composable
internal fun ApplyToAllDialog(
    fileName: String,
    selectedAction: StalledIssueResolutionAction,
    onApplyToCurrent: () -> Unit,
    onApplyToAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    shouldShowApplyToAllOption: Boolean = true,
) {
    val context = LocalContext.current
    val title = getApplyToAllTitle(selectedAction, context)
    val description = getApplyToAllDescription(fileName, selectedAction, context)
    var isApplyToAllChecked by rememberSaveable { mutableStateOf(false) }

    ApplyToAllDialog(
        title = title,
        description = description,
        onApplyToCurrent = onApplyToCurrent,
        onApplyToAll = onApplyToAll,
        onCancel = onCancel,
        modifier = modifier,
        shouldShowApplyToAllOption = shouldShowApplyToAllOption,
        actionButtonStringRes = getActionButtonString(selectedAction),
        isApplyToAllChecked = isApplyToAllChecked,
        onApplyToAllCheckedChange = { isApplyToAllChecked = it },
    )
}


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
    actionButtonStringRes: Int,
    isApplyToAllChecked: Boolean,
    onApplyToAllCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shouldShowApplyToAllOption: Boolean = true,
) {
    BasicDialog(
        modifier = modifier,
        title = SpannableText(title),
        description = SpannableText(
            text = description,
            annotations = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                        spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
                        textColor = TextColor.Primary,
                    ),
                    annotation = null,
                ),
            ),
        ),
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(mega.privacy.android.core.R.string.general_cancel),
                onClick = onCancel,
            ),
            BasicDialogButton(
                text = stringResource(actionButtonStringRes),
                onClick = {
                    if (isApplyToAllChecked) {
                        onApplyToAll()
                    } else {
                        onApplyToCurrent()
                    }
                },
            ),
        ),
        onDismissRequest = onCancel,
        content = if (shouldShowApplyToAllOption) {
            @Composable {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX),
                        checked = isApplyToAllChecked,
                        onCheckStateChanged = onApplyToAllCheckedChange,
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    MegaText(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT),
                        text = stringResource(sharedR.string.sync_apply_to_all_checkbox),
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            null
        },
    )
}

/**
 * Generates a description for the ApplyToAllDialog based on the stalled issue and selected action
 */
private fun getApplyToAllDescription(
    fileName: String,
    selectedAction: StalledIssueResolutionAction,
    context: Context,
): String {

    return when (selectedAction.resolutionActionType) {
        StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE ->
            context.getString(
                sharedR.string.sync_stalled_issue_choose_local_file_explanation,
                fileName
            )

        StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE ->
            context.getString(
                sharedR.string.sync_stalled_issue_choose_remote_file_explanation,
                fileName
            )

        StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME ->
            context.getString(sharedR.string.sync_stalled_issue_choose_last_modified_file_explanation)

        StalledIssueResolutionActionType.RENAME_ALL_ITEMS ->
            context.getString(sharedR.string.sync_stalled_issue_choose_rename_file_explanation)

        StalledIssueResolutionActionType.MERGE_FOLDERS ->
            context.getString(sharedR.string.sync_stalled_issue_choose_merge_folder_explanation)

        else -> ""
    }
}

/**
 * Generates a title for the ApplyToAllDialog based on the selected action
 */
private fun getApplyToAllTitle(
    selectedAction: StalledIssueResolutionAction,
    context: Context,
): String {

    return when (selectedAction.resolutionActionType) {
        StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE ->
            context.getString(sharedR.string.sync_stalled_issue_choose_local_file_title)

        StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE ->
            context.getString(sharedR.string.sync_stalled_issue_choose_remote_file_title)

        StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME ->
            context.getString(sharedR.string.sync_stalled_issue_choose_latest_modified_time_title)

        StalledIssueResolutionActionType.RENAME_ALL_ITEMS ->
            context.getString(sharedR.string.sync_resolve_rename_all_items_title)

        StalledIssueResolutionActionType.MERGE_FOLDERS ->
            context.getString(sharedR.string.sync_stalled_issue_merge_folders_title)

        else -> ""
    }
}

/**
 * Generates a string for the action button based on the selected action
 */
private fun getActionButtonString(
    selectedAction: StalledIssueResolutionAction,
): Int {
    return when (selectedAction.resolutionActionType) {

        StalledIssueResolutionActionType.RENAME_ALL_ITEMS ->
            sharedR.string.context_rename

        StalledIssueResolutionActionType.MERGE_FOLDERS ->
            sharedR.string.sync_apply_all_dialog_merge_button

        else -> sharedR.string.general_dialog_choose_button
    }
}

// Test tags for UI testing
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW = "apply_to_all_dialog:checkbox_row"
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX = "apply_to_all_dialog:checkbox"
internal const val TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT = "apply_to_all_dialog:checkbox_text"

@CombinedThemePreviews
@Composable
internal fun ApplyToAllDialogPreview() {
    AndroidThemeForPreviews {
        ApplyToAllDialog(
            title = "Choose the local file?",
            description = "The local file Let only red flowers bloom.pdf will be moved to the .rubbish or .debris folder in your local sync location.",
            onApplyToCurrent = {},
            onApplyToAll = {},
            onCancel = {},
            actionButtonStringRes = sharedR.string.general_dialog_choose_button,
            isApplyToAllChecked = false,
            onApplyToAllCheckedChange = {},
        )
    }
} 
