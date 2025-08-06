package mega.privacy.android.app.presentation.offline.optionbottomsheet.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.google.common.io.Files
import mega.privacy.android.app.presentation.offline.optionbottomsheet.model.OfflineOptionsUiState
import mega.privacy.android.app.presentation.offline.view.getOfflineNodeDescription
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetContainer
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import java.time.Instant
import kotlin.time.Duration.Companion.days


@Composable
internal fun OfflineOptionsContent(
    uiState: OfflineOptionsUiState,
    fileTypeIconMapper: FileTypeIconMapper,
    onRemoveFromOfflineClicked: () -> Unit,
    onOpenInfoClicked: () -> Unit,
    onOpenWithClicked: (OfflineFileInformation) -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onShareNodeClicked: (OfflineFileInformation) -> Unit,
) {
    if (!uiState.isLoading && uiState.offlineFileInformation != null)
        with(uiState.offlineFileInformation) {
            MegaBottomSheetContainer {
                NodeListViewItem(
                    modifier = Modifier.testTag(NODE_VIEW_TEST_TAG),
                    title = name,
                    titleOverflow = LongTextBehaviour.MiddleEllipsis,
                    subtitle = getOfflineNodeDescription(this@with),
                    icon = if (uiState.offlineFileInformation.isFolder) {
                        R.drawable.ic_folder_medium_solid
                    } else {
                        fileTypeIconMapper(Files.getFileExtension(name))
                    },
                    thumbnailData = thumbnail,
                )
                MegaDivider(dividerType = DividerType.SmallStartPadding)

                MenuActionListTile(
                    modifier = Modifier.testTag(INFO_ACTION_TEST_TAG),
                    text = stringResource(id = mega.privacy.android.app.R.string.general_info),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
                    dividerType = DividerType.BigStartPadding,
                    onActionClicked = {
                        onOpenInfoClicked()
                    },
                )

                if (!isFolder) {
                    MenuActionListTile(
                        modifier = Modifier.testTag(OPEN_WITH_ACTION_TEST_TAG),
                        text = stringResource(id = mega.privacy.android.app.R.string.external_play),
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
                        dividerType = DividerType.BigStartPadding,
                        onActionClicked = {
                            onOpenWithClicked(this@with)
                        },
                    )
                }

                if (uiState.isOnline || !isFolder)
                    MenuActionListTile(
                        modifier = Modifier.testTag(SHARE_ACTION_TEST_TAG),
                        text = stringResource(id = mega.privacy.android.app.R.string.general_share),
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork),
                        dividerType = DividerType.BigStartPadding,
                        onActionClicked = {
                            onShareNodeClicked(this@with)
                        },
                    )

                MenuActionListTile(
                    modifier = Modifier.testTag(SAVE_TO_DEVICE_ACTION_TEST_TAG),
                    text = stringResource(id = mega.privacy.android.app.R.string.general_save_to_device),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download),
                    dividerType = DividerType.BigStartPadding,
                    onActionClicked = {
                        onSaveToDeviceClicked()
                    },
                )
                MenuActionListTile(
                    modifier = Modifier.testTag(REMOVE_FROM_OFFLINE_ACTION_TEST_TAG),
                    text = stringResource(id = mega.privacy.android.app.R.string.context_delete_offline),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    dividerType = null,
                    onActionClicked = {
                        onRemoveFromOfflineClicked()
                    },
                )
            }
        }
}

internal const val NODE_VIEW_TEST_TAG = "offline_options_content:node_view"
internal const val INFO_ACTION_TEST_TAG = "offline_options_content:info_action"
internal const val OPEN_WITH_ACTION_TEST_TAG = "offline_options_content:open_with_action"
internal const val SHARE_ACTION_TEST_TAG = "offline_options_content:share_action"
internal const val SAVE_TO_DEVICE_ACTION_TEST_TAG = "offline_options_content:save_to_device_action"
internal const val REMOVE_FROM_OFFLINE_ACTION_TEST_TAG =
    "offline_options_context:remove_from_offline_action"

@CombinedThemePreviews
@Composable
private fun OfflineOptionsContentPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OfflineOptionsContent(
            uiState = OfflineOptionsUiState(
                nodeId = NodeId(1),
                isOnline = true,
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = OtherOfflineNodeInformation(
                        id = 1,
                        parentId = 0,
                        handle = "1234",
                        name = "Title.txt",
                        isFolder = false,
                        lastModifiedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
                        path = ""
                    ),
                    thumbnail = null,
                ),
                isLoading = false
            ),
            fileTypeIconMapper = FileTypeIconMapper(),
            onRemoveFromOfflineClicked = {},
            onOpenInfoClicked = {},
            onOpenWithClicked = {},
            onSaveToDeviceClicked = {},
            onShareNodeClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OfflineOptionsContentFolderPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OfflineOptionsContent(
            uiState = OfflineOptionsUiState(
                nodeId = NodeId(1),
                isOnline = false,
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = OtherOfflineNodeInformation(
                        id = 1,
                        parentId = 0,
                        handle = "1234",
                        name = "My Folder",
                        isFolder = true,
                        lastModifiedTime = Instant.now().epochSecond - 10.days.inWholeSeconds,
                        path = ""
                    ),
                    thumbnail = null,
                ),
                isLoading = false
            ),
            fileTypeIconMapper = FileTypeIconMapper(),
            onRemoveFromOfflineClicked = {},
            onOpenInfoClicked = {},
            onOpenWithClicked = {},
            onSaveToDeviceClicked = {},
            onShareNodeClicked = {}
        )
    }
}