package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedResR

internal const val OFFLINE_OPTIONS_NODE_LIST_VIEW_ITEM = "offline_options:node_list_view_item"
internal const val OFFLINE_OPTIONS_INFO_MENU_ITEM = "offline_options:info_menu_item"
internal const val OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM = "offline_options:open_with_menu_item"
internal const val OFFLINE_OPTIONS_SHARE_MENU_ITEM = "offline_options:share_menu_item"
internal const val OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM = "offline_options:save_to_device"
internal const val OFFLINE_OPTIONS_DELETE_MENU_ITEM = "offline_options:delete_menu_item"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineOptionsBottomSheet(
    offlineFileInformation: OfflineFileInformation,
    onShareOfflineFile: () -> Unit,
    onSaveOfflineFileToDevice: () -> Unit,
    onDeleteOfflineFile: () -> Unit,
    onOpenOfflineFile: () -> Unit,
    onOpenWithFile: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )

    MegaModalBottomSheet(
        modifier = modifier,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        OfflineOptionsBottomSheetContent(
            offlineFileInformation = offlineFileInformation,
            onShareOfflineFile = onShareOfflineFile,
            onSaveOfflineFileToDevice = onSaveOfflineFileToDevice,
            onDeleteOfflineFile = onDeleteOfflineFile,
            onOpenOfflineFile = onOpenOfflineFile,
            onOpenWithFile = onOpenWithFile,
            isOnline = isOnline
        )
    }
}

@Composable
internal fun OfflineOptionsBottomSheetContent(
    offlineFileInformation: OfflineFileInformation,
    onShareOfflineFile: () -> Unit,
    onSaveOfflineFileToDevice: () -> Unit,
    onDeleteOfflineFile: () -> Unit,
    onOpenOfflineFile: () -> Unit,
    onOpenWithFile: () -> Unit,
    isOnline: Boolean = false,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
    ) {
        with(offlineFileInformation) {
            NodeListViewItem(
                modifier = Modifier
                    .testTag(OFFLINE_OPTIONS_NODE_LIST_VIEW_ITEM)
                    .fillMaxWidth(),
                title = name,
                subtitle = getOfflineNodeDescription(offlineFileInformation),
                icon = if (isFolder) {
                    iconPackR.drawable.ic_folder_medium_solid
                } else {
                    getFileTypeIcon(offlineFileInformation.name) ?: return@with
                },
                thumbnailData = thumbnail,
                onItemClicked = {}
            )

            NodeActionListTile(
                modifier = Modifier
                    .testTag(OFFLINE_OPTIONS_INFO_MENU_ITEM)
                    .fillMaxWidth(),
                text = stringResource(sharedResR.string.general_info),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
                onActionClicked = onOpenOfflineFile
            )

            if (!isFolder) {
                NodeActionListTile(
                    modifier = Modifier
                        .testTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM)
                        .fillMaxWidth(),
                    text = stringResource(sharedResR.string.external_play),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
                    onActionClicked = onOpenWithFile
                )
            }

            if (isOnline || !isFolder) {
                NodeActionListTile(
                    modifier = Modifier
                        .testTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM)
                        .fillMaxWidth(),
                    text = stringResource(sharedResR.string.general_share),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork),
                    onActionClicked = onShareOfflineFile
                )
            }

            NodeActionListTile(
                modifier = Modifier
                    .testTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM)
                    .fillMaxWidth(),
                text = stringResource(sharedResR.string.general_save_to_device),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download),
                onActionClicked = onSaveOfflineFileToDevice
            )

            NodeActionListTile(
                modifier = Modifier
                    .testTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM)
                    .fillMaxWidth(),
                text = stringResource(R.string.offline_screen_selection_menu_remove_from_offline),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                onActionClicked = onDeleteOfflineFile
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewOfflineOptionsBottomSheet() {
    AndroidThemeForPreviews {
        OfflineOptionsBottomSheet(
            offlineFileInformation = OfflineFileInformation(
                nodeInfo = OtherOfflineNodeInformation(
                    id = 1,
                    path = "/storage/emulated/0/Mega/offline/filename.jpg",
                    name = "filename.jpg",
                    handle = "123456789",
                    isFolder = false,
                    lastModifiedTime = 1655292000000,
                    parentId = -1
                )
            ),
            onShareOfflineFile = {},
            onSaveOfflineFileToDevice = {},
            onDeleteOfflineFile = {},
            onOpenOfflineFile = {},
            onOpenWithFile = {},
            onDismiss = {}
        )
    }
}