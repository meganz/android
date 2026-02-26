package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.InfoBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.RemoveConnectionBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.lists.getStatusText
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.original.core.ui.controls.lists.StatusListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * A [Composable] that represents the Body of the Folder Bottom Sheet
 *
 * @param folder The [DeviceFolderUINode] to be displayed
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 * @param onRemoveConnectionClicked Lambda that is executed when the "Remove connection" Tile is selected
 */
@Composable
internal fun DeviceFolderBottomSheetBody(
    folder: DeviceFolderUINode,
    onInfoClicked: () -> Unit,
    onRemoveConnectionClicked: () -> Unit,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_FOLDER)) {
        StatusListViewItem(
            icon = folder.icon.iconRes,
            name = folder.name,
            statusText = getStatusText(folder.status),
            modifier = Modifier.padding(bottom = 8.dp),
            applySecondaryColorIconTint = folder.icon.applySecondaryColorTint,
            statusIcon = folder.status.icon,
            statusColor = folder.status.color,
        )
        InfoBottomSheetTile(onActionClicked = onInfoClicked)
        RemoveConnectionBottomSheetTile(
            onActionClicked = onRemoveConnectionClicked,
            dividerType = null,
        )
    }
}

/**
 * Preview parameter provider for DeviceFolderBottomSheetBody
 */
private class FolderBottomSheetPreviewProvider : PreviewParameterProvider<DeviceFolderUINode> {
    override val values: Sequence<DeviceFolderUINode> = sequenceOf(
        NonBackupDeviceFolderUINode(
            id = "1",
            name = "Camera Uploads",
            icon = FolderIconType.CameraUploads,
            status = DeviceCenterUINodeStatus.UpToDate,
            rootHandle = 123456L,
            localFolderPath = "/storage/emulated/0/DCIM/Camera"
        ),
        NonBackupDeviceFolderUINode(
            id = "2",
            name = "Documents",
            icon = FolderIconType.Folder,
            status = DeviceCenterUINodeStatus.Updating,
            rootHandle = 789012L,
            localFolderPath = "/storage/emulated/0/Documents"
        ),
        NonBackupDeviceFolderUINode(
            id = "3",
            name = "Sync Folder",
            icon = FolderIconType.Sync,
            status = DeviceCenterUINodeStatus.UpdatingWithPercentage(75),
            rootHandle = 345678L,
            localFolderPath = "/storage/emulated/0/Sync"
        )
    )
}

/**
 * Preview for [DeviceFolderBottomSheetBody]
 */
@CombinedThemePreviews
@Composable
private fun DeviceFolderBottomSheetBodyPreview(
    @PreviewParameter(FolderBottomSheetPreviewProvider::class) folder: DeviceFolderUINode,
) {
    AndroidThemeForPreviews {
        DeviceFolderBottomSheetBody(
            folder = folder,
            onInfoClicked = {},
            onRemoveConnectionClicked = {}
        )
    }
}

/**
 * Test Tag for the Folder Bottom Sheet Body
 */
internal const val BOTTOM_SHEET_BODY_FOLDER =
    "folder_bottom_sheet_body:column_options_list"
