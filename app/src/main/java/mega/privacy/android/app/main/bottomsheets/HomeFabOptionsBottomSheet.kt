package mega.privacy.android.app.main.bottomsheets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.HomeNewTextFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFilesMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFolderMenuToolbarEvent

@Composable
fun HomeFabOptionsBottomSheet(
    onUploadFilesClicked: () -> Unit,
    onUploadFolderClicked: () -> Unit,
    onScanDocumentClicked: () -> Unit,
    onCaptureClicked: () -> Unit,
    onCreateNewTextFileClicked: () -> Unit,
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onNewChatClicked: () -> Unit,
    hideSheet: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        MegaText(
            text = stringResource(R.string.context_upload),
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.subtitle2,
        )
        MenuActionListTile(
            text = stringResource(R.string.upload_files),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.FileUpload),
            dividerType = null,
            onActionClicked = {
                Analytics.tracker.trackEvent(HomeUploadFilesMenuToolbarEvent)
                onUploadFilesClicked()
                hideSheet()
            },
        )
        MenuActionListTile(
            text = stringResource(R.string.upload_folder),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.FolderArrow),
            dividerType = DividerType.BigStartPadding,
            onActionClicked = {
                Analytics.tracker.trackEvent(HomeUploadFolderMenuToolbarEvent)
                onUploadFolderClicked()
                hideSheet()
            },
        )
        MenuActionListTile(
            text = stringResource(R.string.menu_scan_document),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.FileScan),
            dividerType = null,
            onActionClicked = {
                onScanDocumentClicked()
                hideSheet()
            },
        )
        MenuActionListTile(
            text = stringResource(R.string.menu_take_picture),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Camera),
            dividerType = DividerType.BigStartPadding,
            onActionClicked = {
                onCaptureClicked()
                hideSheet()
            },
        )
        MenuActionListTile(
            text = stringResource(R.string.action_create_txt),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.FilePlus02),
            dividerType = null,
            onActionClicked = {
                Analytics.tracker.trackEvent(HomeNewTextFileMenuToolbarEvent)
                onCreateNewTextFileClicked()
                hideSheet()
            },
        )
        MegaText(
            text = stringResource(sharedResR.string.settings_section_sync),
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.subtitle2,
        )
        MenuActionListTile(
            text = stringResource(sharedResR.string.device_center_sync_add_new_syn_button_option),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Sync01),
            dividerType = null,
            onActionClicked = {
                onAddNewSyncClicked()
                hideSheet()
            },
        )
        MenuActionListTile(
            text = stringResource(sharedResR.string.device_center_sync_add_new_backup_button_option),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Database),
            dividerType = null,
            onActionClicked = {
                onAddNewBackupClicked()
                hideSheet()
            },
        )
        MegaText(
            text = stringResource(R.string.section_chat),
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.subtitle2,
        )
        MenuActionListTile(
            text = stringResource(R.string.fab_label_new_chat),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageChatCircle),
            dividerType = null,
            onActionClicked = {
                onNewChatClicked()
                hideSheet()
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewOwnDeviceBottomSheet() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        HomeFabOptionsBottomSheet(
            onUploadFilesClicked = {},
            onUploadFolderClicked = {},
            onScanDocumentClicked = {},
            onCaptureClicked = {},
            onCreateNewTextFileClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            onNewChatClicked = {},
        )
    }
}