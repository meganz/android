package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.google.common.io.Files
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.ChatAttachmentUiEntity
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.NodeAttachmentBottomSheetUiState
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_AVAILABLE_OFFLINE_SWITCH
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetContainer
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun NodeAttachmentBottomSheetContent(
    uiState: NodeAttachmentBottomSheetUiState,
    fileTypeIconMapper: FileTypeIconMapper,
    onAvailableOfflineChecked: (Boolean, NodeId) -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onImportClicked: () -> Unit,
) {
    if (!uiState.isLoading && uiState.item != null)
        with(uiState.item) {
            MegaBottomSheetContainer {
                NodeListViewItem(
                    modifier = Modifier.testTag(NODE_VIEW_TEST_TAG),
                    title = name,
                    titleOverflow = LongTextBehaviour.MiddleEllipsis,
                    subtitle = formatFileSize(size, LocalContext.current),
                    icon = fileTypeIconMapper(Files.getFileExtension(name)),
                    thumbnailData = thumbnailPath,
                )
                MegaDivider(dividerType = DividerType.SmallStartPadding)

                if (!isInAnonymousMode) {
                    MenuActionListTile(
                        modifier = Modifier.testTag(ADD_TO_CLOUD_DRIVE_ACTION_TEST_TAG),
                        text = stringResource(id = mega.privacy.android.app.R.string.add_to_cloud_node_chat),
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudUpload),
                        dividerType = DividerType.BigStartPadding,
                        onActionClicked = onImportClicked,
                    )
                }

                MenuActionListTile(
                    modifier = Modifier.testTag(SAVE_TO_DEVICE_ACTION_TEST_TAG),
                    text = stringResource(id = mega.privacy.android.app.R.string.general_save_to_device),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download),
                    dividerType = DividerType.BigStartPadding,
                    onActionClicked = onSaveToDeviceClicked,
                )

                if (!isInAnonymousMode) {
                    MenuActionListTile(
                        modifier = Modifier.testTag(AVAILABLE_OFFLINE_ACTION_TEST_TAG),
                        text = stringResource(id = mega.privacy.android.app.R.string.file_properties_available_offline),
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ArrowDownCircle),
                        dividerType = null,
                        trailingItem = {
                            MegaSwitch(
                                checked = isAvailableOffline,
                                enabled = true,
                                onCheckedChange = {
                                    onAvailableOfflineChecked(it, nodeId)
                                },
                                modifier = Modifier.testTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH)
                            )
                        }
                    )
                }
            }
        }
}

internal const val NODE_VIEW_TEST_TAG = "node_attachment:node_view"
internal const val ADD_TO_CLOUD_DRIVE_ACTION_TEST_TAG = "node_attachment:add_to_cloud_drive_action"
internal const val SAVE_TO_DEVICE_ACTION_TEST_TAG = "node_attachment:save_to_device_action"
internal const val AVAILABLE_OFFLINE_ACTION_TEST_TAG = "node_attachment:available_offline_action"

@CombinedThemePreviews
@Composable
private fun NodeAttachmentBottomSheetContentPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeAttachmentBottomSheetContent(
            uiState = NodeAttachmentBottomSheetUiState(
                isOnline = false,
                item = ChatAttachmentUiEntity(
                    nodeId = NodeId(123),
                    name = "Title",
                    size = 1230,
                    thumbnailPath = null,
                    isAvailableOffline = true,
                    isInAnonymousMode = false
                ),
                isLoading = false
            ),
            fileTypeIconMapper = FileTypeIconMapper(),
            onAvailableOfflineChecked = { _, _ -> },
            onSaveToDeviceClicked = {},
            onImportClicked = {}
        )
    }
}