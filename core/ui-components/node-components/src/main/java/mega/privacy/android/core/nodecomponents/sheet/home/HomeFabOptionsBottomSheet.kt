package mega.privacy.android.core.nodecomponents.sheet.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.list.HeaderTextStyle
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionItem
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.HomeAddNewBackupMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeAddNewSyncMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeCaptureMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeNewChatMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeNewTextFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeScanDocumentMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFilesMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFolderMenuToolbarEvent

/**
 * Material 3 bottom sheet for home FAB options with Upload, Sync, and Chat sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFabOptionsBottomSheet(
    modifier: Modifier = Modifier,
    onUploadFilesClicked: () -> Unit,
    onUploadFolderClicked: () -> Unit,
    onScanDocumentClicked: () -> Unit,
    onCaptureClicked: () -> Unit,
    onCreateNewTextFileClicked: () -> Unit,
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onNewChatClicked: () -> Unit,
) {
    Column(
        modifier = modifier.testTag(TEST_TAG_HOME_FAB_OPTIONS_SHEET)
    ) {
        // Upload section
        SecondaryHeaderListItem(
            modifier = Modifier.testTag(TEST_TAG_UPLOAD_SECTION_HEADER),
            text = stringResource(R.string.context_upload),
            headerTextStyle = HeaderTextStyle.Medium,
            enableClick = false
        )
        UploadOptionItem(
            text = stringResource(id = R.string.upload_files),
            icon = IconPack.Medium.Thin.Outline.FileUpload,
            testTag = TEST_TAG_UPLOAD_FILES_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeUploadFilesMenuToolbarEvent)
                onUploadFilesClicked()
            },
        )
        UploadOptionItem(
            text = stringResource(id = R.string.upload_folder),
            icon = IconPack.Medium.Thin.Outline.FolderArrow,
            testTag = TEST_TAG_UPLOAD_FOLDER_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeUploadFolderMenuToolbarEvent)
                onUploadFolderClicked()
            },
        )
        UploadOptionItem(
            text = stringResource(id = R.string.menu_scan_document),
            icon = IconPack.Medium.Thin.Outline.FileScan,
            testTag = TEST_TAG_SCAN_DOCUMENT_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeScanDocumentMenuToolbarEvent)
                onScanDocumentClicked()
            },
        )
        UploadOptionItem(
            text = stringResource(id = R.string.menu_take_picture),
            icon = IconPack.Medium.Thin.Outline.Camera,
            testTag = TEST_TAG_CAPTURE_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeCaptureMenuToolbarEvent)
                onCaptureClicked()
            },
        )
        UploadOptionItem(
            text = stringResource(id = R.string.action_create_txt),
            icon = IconPack.Medium.Thin.Outline.FilePlus02,
            testTag = TEST_TAG_NEW_TEXT_FILE_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeNewTextFileMenuToolbarEvent)
                onCreateNewTextFileClicked()
            },
        )

        // Sync section
        SecondaryHeaderListItem(
            modifier = Modifier.testTag(TEST_TAG_SYNC_SECTION_HEADER),
            text = stringResource(id = sharedResR.string.settings_section_sync),
            headerTextStyle = HeaderTextStyle.Medium,
            enableClick = false
        )
        UploadOptionItem(
            text = stringResource(id = sharedResR.string.device_center_sync_add_new_syn_button_option),
            icon = IconPack.Medium.Thin.Outline.Sync01,
            testTag = TEST_TAG_ADD_NEW_SYNC_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeAddNewSyncMenuToolbarEvent)
                onAddNewSyncClicked()
            },
        )
        UploadOptionItem(
            text = stringResource(id = sharedResR.string.device_center_sync_add_new_backup_button_option),
            icon = IconPack.Medium.Thin.Outline.Database,
            testTag = TEST_TAG_ADD_NEW_BACKUP_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeAddNewBackupMenuToolbarEvent)
                onAddNewBackupClicked()
            },
        )

        // Chat section
        SecondaryHeaderListItem(
            modifier = Modifier.testTag(TEST_TAG_CHAT_SECTION_HEADER),
            text = stringResource(id = sharedResR.string.general_chat),
            headerTextStyle = HeaderTextStyle.Medium,
            enableClick = false
        )
        UploadOptionItem(
            text = stringResource(R.string.fab_label_new_chat),
            icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
            testTag = TEST_TAG_NEW_CHAT_ACTION,
            onClick = {
                Analytics.tracker.trackEvent(HomeNewChatMenuToolbarEvent)
                onNewChatClicked()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun HomeFabOptionsBottomSheetPreview() {
    AndroidThemeForPreviews {
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

internal const val TEST_TAG_HOME_FAB_OPTIONS_SHEET = "home_fab_options_sheet"
internal const val TEST_TAG_UPLOAD_SECTION_HEADER =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:upload_section_header"
internal const val TEST_TAG_UPLOAD_FILES_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:upload_files_action"
internal const val TEST_TAG_UPLOAD_FOLDER_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:upload_folder_action"
internal const val TEST_TAG_SCAN_DOCUMENT_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:scan_document_action"
internal const val TEST_TAG_CAPTURE_ACTION = "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:capture_action"
internal const val TEST_TAG_NEW_TEXT_FILE_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:new_text_file_action"
internal const val TEST_TAG_SYNC_SECTION_HEADER =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:sync_section_header"
internal const val TEST_TAG_ADD_NEW_SYNC_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:add_new_sync_action"
internal const val TEST_TAG_ADD_NEW_BACKUP_ACTION =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:add_new_backup_action"
internal const val TEST_TAG_CHAT_SECTION_HEADER =
    "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:chat_section_header"
internal const val TEST_TAG_NEW_CHAT_ACTION = "$TEST_TAG_HOME_FAB_OPTIONS_SHEET:new_chat_action"

