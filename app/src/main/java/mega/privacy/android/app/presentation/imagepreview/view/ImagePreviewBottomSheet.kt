@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.core.R.drawable.link_ic
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
internal fun ImagePreviewBottomSheetPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.HalfExpanded,
            skipHalfExpanded = false,
        )
        ImagePreviewBottomSheet(
            modalSheetState = modalSheetState,
        )
    }
}

@Composable
internal fun ImagePreviewBottomSheet(
    modalSheetState: ModalBottomSheetState,
    imageName: String = "",
    imageInfo: String = "",
    imageThumbnailPath: String? = "",
    onInfoClicked: () -> Unit = {},
    onFavouriteClicked: () -> Unit = {},
    onDisputeTakeDownClicked: () -> Unit = {},
    onSlideShowClicked: () -> Unit = {},
    onOpenWithClicked: () -> Unit = {},
    onForwardClicked: () -> Unit = {},
    onSaveToDeviceClicked: () -> Unit = {},
    onAvailableOfflineClicked: () -> Unit = {},
    onManageLinkClicked: () -> Unit = {},
    onRemoveLinkClicked: () -> Unit = {},
    onSendToClicked: () -> Unit = {},
    onShareClicked: () -> Unit = {},
    onRenameClicked: () -> Unit = {},
    onMoveClicked: () -> Unit = {},
    onCopyClicked: () -> Unit = {},
    onRestoreClicked: () -> Unit = {},
    onRemoveFromOfflineClicked: () -> Unit = {},
    onRemoveClicked: () -> Unit = {},
    onMoveToRubbishBinClicked: () -> Unit = {},
    content: (@Composable () -> Unit)? = null,
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = {
            ImagePreviewMenuActionHeader(
                imageName = imageName,
                imageInfo = imageInfo,
                imageThumbnailPath = imageThumbnailPath,
            )
        },
        sheetBody = {
            Column {
                MenuActionListTile(
                    icon = R.drawable.info_ic,
                    text = stringResource(id = R.string.general_info),
                    onActionClicked = onInfoClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_add_favourite,
                    text = stringResource(id = R.string.file_properties_favourite),
                    onActionClicked = onFavouriteClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_label,
                    text = stringResource(id = R.string.file_properties_label),
                    onActionClicked = onFavouriteClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_taken_down_bottom_sheet,
                    text = stringResource(id = R.string.dispute_takendown_file),
                    onActionClicked = onDisputeTakeDownClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_slideshow,
                    text = stringResource(id = R.string.action_slideshow),
                    onActionClicked = onSlideShowClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_open_with,
                    text = stringResource(id = R.string.external_play),
                    onActionClicked = onOpenWithClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_forward,
                    text = stringResource(id = R.string.forward_menu_item),
                    onActionClicked = onForwardClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_save_to_device,
                    text = stringResource(id = R.string.general_save_to_device),
                    onActionClicked = onSaveToDeviceClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_save_offline,
                    text = stringResource(id = R.string.file_properties_available_offline),
                    onActionClicked = onAvailableOfflineClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = link_ic,
                    text = stringResource(id = R.string.edit_link_option),
                    onActionClicked = onManageLinkClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_remove_link,
                    text = stringResource(id = R.string.context_remove_link_menu),
                    onActionClicked = onRemoveLinkClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_send_to_contact,
                    text = stringResource(id = R.string.context_send_file_to_chat),
                    onActionClicked = onSendToClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_social_share_white,
                    text = stringResource(id = R.string.general_share),
                    onActionClicked = onShareClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_rename,
                    text = stringResource(id = R.string.context_rename),
                    onActionClicked = onRenameClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_move,
                    text = stringResource(id = R.string.general_move),
                    onActionClicked = onMoveClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_menu_copy,
                    text = stringResource(id = R.string.context_copy),
                    onActionClicked = onCopyClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_restore,
                    text = stringResource(id = R.string.context_restore),
                    onActionClicked = onRestoreClicked,
                )
                Divider(startIndent = 72.dp)
                MenuActionListTile(
                    icon = R.drawable.ic_remove,
                    text = stringResource(id = R.string.context_delete_offline),
                    onActionClicked = onRemoveFromOfflineClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_remove,
                    text = stringResource(id = R.string.context_remove),
                    onActionClicked = onRemoveClicked,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_rubbish_bin,
                    text = stringResource(id = R.string.context_move_to_trash),
                    onActionClicked = onMoveToRubbishBinClicked,
                )
            }
        },
        content = content,
    )
}

@Composable
internal fun ImagePreviewMenuActionHeader(
    imageName: String,
    imageInfo: String,
    imageThumbnailPath: String?,
) {
    Row(
        modifier = Modifier.padding(all = 16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageThumbnailPath)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = android.R.drawable.ic_menu_camera),
            error = painterResource(id = android.R.drawable.ic_menu_camera),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp, 40.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text(
                text = imageName,
                color = MaterialTheme.colors.textColorPrimary,
            )
            Text(
                text = imageInfo,
                color = MaterialTheme.colors.textColorSecondary,
                maxLines = 1,
            )
        }
    }
}