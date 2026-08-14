package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun StopBackupConfirmationDialog(
    onConfirm: (option: StopBackupOption, selectedFolder: RemoteFolder?) -> Unit,
    onDismiss: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    folderName: String?,
    modifier: Modifier = Modifier,
) {

    StopBackupConfirmationDialogBody(
        onConfirm = onConfirm,
        onDismiss = {
            onDismiss()
        },
        folderName = folderName,
        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
        modifier = modifier,
    )
}

@Composable
internal fun StopBackupConfirmationDialogBody(
    onConfirm: (option: StopBackupOption, selectedFolder: RemoteFolder?) -> Unit,
    onDismiss: () -> Unit,
    folderName: String?,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG),
        title = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_title),
        description = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_text),
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_move_cloud_drive),
                onClick = { onSelectStopBackupDestinationClicked(folderName) },
            ),
            BasicDialogButton(
                text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_delete_permanently),
                onClick = { onConfirm(StopBackupOption.DELETE, null) },
            ),
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            ),
        ),
        onDismissRequest = onDismiss,
        buttonDirection = VERTICAL,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MegaText(
                text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_move_explanation),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
            MegaText(
                text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_delete_explanation),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun StopBackupConfirmationDialogBodyPreview() {
    AndroidThemeForPreviews {
        StopBackupConfirmationDialogBody(
            onConfirm = { _, _ -> },
            onDismiss = {},
            folderName = null,
            onSelectStopBackupDestinationClicked = {},
        )
    }
}

internal const val STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG =
    "stop_backup_confirmation_dialog:body"
