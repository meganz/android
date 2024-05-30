package mega.privacy.android.app.presentation.offline.optionbottomsheet.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.google.common.io.Files
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.app.presentation.offline.view.getOfflineNodeDescription
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetContainer
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme


@Composable
internal fun OfflineOptionsContent(
    uiState: OfflineFileInfoUiState,
    fileTypeIconMapper: FileTypeIconMapper,
    onRemoveFromOfflineClicked: () -> Unit,
    onOpenInfoClicked: () -> Unit,
    onOpenWithClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onShareNodeClicked: () -> Unit,
) {
    if (!uiState.isLoading)
        MegaBottomSheetContainer {
            NodeListViewItem(
                modifier = Modifier.testTag(NODE_VIEW_TEST_TAG),
                title = uiState.title,
                titleOverflow = LongTextBehaviour.MiddleEllipsis,
                subtitle = getOfflineNodeDescription(uiState),
                icon = if (uiState.isFolder) {
                    R.drawable.ic_folder_medium_solid
                } else {
                    fileTypeIconMapper(Files.getFileExtension(uiState.title))
                },
                thumbnailData = uiState.thumbnail,
            )
            MegaDivider(dividerType = DividerType.SmallStartPadding)

            MenuActionListTile(
                modifier = Modifier.testTag(INFO_ACTION_TEST_TAG),
                text = stringResource(id = mega.privacy.android.app.R.string.general_info),
                icon = painterResource(id = R.drawable.ic_info_medium_regular_outline),
                dividerType = DividerType.BigStartPadding,
                onActionClicked = {
                    onOpenInfoClicked()
                },
            )

            if (!uiState.isFolder) {
                MenuActionListTile(
                    modifier = Modifier.testTag(OPEN_WITH_ACTION_TEST_TAG),
                    text = stringResource(id = mega.privacy.android.app.R.string.external_play),
                    icon = painterResource(id = R.drawable.ic_external_link_medium_regular_outline),
                    dividerType = DividerType.BigStartPadding,
                    onActionClicked = {
                        onOpenWithClicked()
                    },
                )
            }

            MenuActionListTile(
                modifier = Modifier.testTag(SHARE_ACTION_TEST_TAG),
                text = stringResource(id = mega.privacy.android.app.R.string.general_share),
                icon = painterResource(id = R.drawable.ic_share_network_medium_regular_outline),
                dividerType = DividerType.BigStartPadding,
                onActionClicked = {
                    onShareNodeClicked()
                },
            )
            MenuActionListTile(
                modifier = Modifier.testTag(SAVE_TO_DEVICE_ACTION_TEST_TAG),
                text = stringResource(id = mega.privacy.android.app.R.string.general_save_to_device),
                icon = painterResource(id = R.drawable.ic_download_medium_regular_outline),
                dividerType = DividerType.BigStartPadding,
                onActionClicked = {
                    onSaveToDeviceClicked()
                },
            )
            MenuActionListTile(
                modifier = Modifier.testTag(REMOVE_FROM_OFFLINE_ACTION_TEST_TAG),
                text = stringResource(id = mega.privacy.android.app.R.string.context_delete_offline),
                icon = painterResource(id = R.drawable.ic_x_medium_regular_outline),
                dividerType = null,
                onActionClicked = {
                    onRemoveFromOfflineClicked()
                },
            )
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OfflineOptionsContent(
            uiState = OfflineFileInfoUiState(
                title = "Title",
                isFolder = false,
                thumbnail = null,
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