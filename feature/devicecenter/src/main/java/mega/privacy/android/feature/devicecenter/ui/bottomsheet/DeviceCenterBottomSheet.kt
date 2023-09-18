package mega.privacy.android.feature.devicecenter.ui.bottomsheet


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.lists.MenuActionNodeHeaderWithBody
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BackupFolderBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.NonBackupFolderBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.OtherDeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.OwnDeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * Test Tags for the Device Center Bottom Sheet
 */
internal const val BOTTOM_SHEET_CONTAINER =
    "device_center_bottom_sheet:menu_action_bottom_sheet_container"
internal const val BOTTOM_SHEET_HEADER =
    "device_center_bottom_sheet:menu_action_node_header_with_body_node_information"

/**
 * A [Composable] Bottom Sheet shown when clicking a Device Center Node's Menu Icon
 *
 * Various options are displayed depending on the [selectedNode]
 *
 * @param coroutineScope The [CoroutineScope] used to hide the Bottom Sheet
 * @param modalSheetState The Bottom Sheet State
 * @param selectedNode The selected [DeviceCenterUINode]
 * @param isCameraUploadsEnabled true if Camera Uploads is Enabled, and false if otherwise
 * @param onCameraUploadsClicked Lambda that is executed when the "Camera uploads" Tile is selected
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onShowInBackupsClicked Lambda that is executed when the "Show in Backups" Tile is selected
 * @param onShowInCloudDriveClicked Lambda that is executed when the "Show in Cloud Drive" Tile is
 * selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DeviceCenterBottomSheet(
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
    selectedNode: DeviceCenterUINode,
    isCameraUploadsEnabled: Boolean,
    onCameraUploadsClicked: () -> Unit,
    onRenameDeviceClicked: (DeviceUINode) -> Unit,
    onShowInBackupsClicked: () -> Unit,
    onShowInCloudDriveClicked: () -> Unit,
    onInfoClicked: () -> Unit,
) {
    BottomSheet(
        modifier = Modifier.testTag(BOTTOM_SHEET_CONTAINER),
        modalSheetState = modalSheetState,
        sheetHeader = {
            MenuActionNodeHeaderWithBody(
                modifier = Modifier.testTag(BOTTOM_SHEET_HEADER),
                title = selectedNode.name,
                body = stringResource(selectedNode.status.name),
                nodeIcon = selectedNode.icon.iconRes,
                bodyIcon = selectedNode.status.icon,
                bodyColor = selectedNode.status.color ?: MaterialTheme.colors.textColorSecondary,
                bodyIconColor = selectedNode.status.color ?: Color.Unspecified,
                nodeIconColor = if (selectedNode.icon.applySecondaryColorTint) {
                    MaterialTheme.colors.textColorSecondary
                } else {
                    null
                },
            )
        },
        sheetBody = {
            // Display the specific Bottom Sheet Body depending on the selected Node's type
            when (selectedNode) {
                is OwnDeviceUINode -> {
                    OwnDeviceBottomSheetBody(
                        isCameraUploadsEnabled = isCameraUploadsEnabled,
                        onCameraUploadsClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onCameraUploadsClicked.invoke()
                        },
                        onRenameDeviceClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRenameDeviceClicked(selectedNode)
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }

                is OtherDeviceUINode -> {
                    OtherDeviceBottomSheetBody(
                        onRenameDeviceClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRenameDeviceClicked(selectedNode)
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }

                is BackupDeviceFolderUINode -> {
                    BackupFolderBottomSheetBody(
                        onShowInBackupsClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onShowInBackupsClicked.invoke()
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }

                is NonBackupDeviceFolderUINode -> {
                    NonBackupFolderBottomSheetBody(
                        onShowInCloudDriveClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onShowInCloudDriveClicked.invoke()
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }
            }
        },
    )
}

/**
 * A Preview Composable that displays the Bottom Sheet and its options
 */
@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterBottomSheet() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Test Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        DeviceCenterBottomSheet(
            coroutineScope = rememberCoroutineScope(),
            modalSheetState = ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                isSkipHalfExpanded = false,
            ),
            selectedNode = ownDeviceUINode,
            isCameraUploadsEnabled = true,
            onCameraUploadsClicked = {},
            onRenameDeviceClicked = {},
            onShowInBackupsClicked = {},
            onShowInCloudDriveClicked = {},
            onInfoClicked = {},
        )
    }
}