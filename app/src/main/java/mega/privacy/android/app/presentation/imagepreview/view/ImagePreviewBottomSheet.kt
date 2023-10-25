@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

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
    isFavourite: Boolean = false,
    imageThumbnailPath: String? = "",
    onClickInfo: () -> Unit = {},
    onClickFavourite: () -> Unit = {},
    onClickLabel: () -> Unit = {},
    onClickDisputeTakeDown: () -> Unit = {},
    onClickSlideShow: () -> Unit = {},
    onClickOpenWith: () -> Unit = {},
    onClickForward: () -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onSwitchAvailableOffline: ((Boolean) -> Unit)? = null,
    onClickGetLink: () -> Unit = {},
    onClickRemoveLink: () -> Unit = {},
    onClickSendTo: () -> Unit = {},
    onClickShare: () -> Unit = {},
    onClickRename: () -> Unit = {},
    onClickMove: () -> Unit = {},
    onClickCopy: () -> Unit = {},
    onClickRestore: () -> Unit = {},
    onClickRemoveFromOffline: () -> Unit = {},
    onClickRemove: () -> Unit = {},
    onClickMoveToRubbishBin: () -> Unit = {},
    showDisputeTakeDown: Boolean = false,
    showSlideShow: Boolean = false,
    showForward: Boolean = false,
    showRemoveLink: Boolean = false,
    showRestore: Boolean = false,
    showRemoveFromOffline: Boolean = false,
    showRemove: Boolean = false,
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
                    onActionClicked = onClickInfo,
                    addSeparator = false,
                )
                MenuActionListTile(
                    icon = if (isFavourite) R.drawable.ic_remove_favourite
                    else R.drawable.ic_add_favourite,
                    text = if (isFavourite) stringResource(id = R.string.file_properties_unfavourite)
                    else stringResource(id = R.string.file_properties_favourite),
                    onActionClicked = onClickFavourite,
                    addSeparator = false,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_label,
                    text = stringResource(id = R.string.file_properties_label),
                    onActionClicked = onClickLabel,
                    addSeparator = true,
                )
                if (showDisputeTakeDown) {
                    MenuActionListTile(
                        icon = R.drawable.ic_taken_down_bottom_sheet,
                        text = stringResource(id = R.string.dispute_takendown_file),
                        onActionClicked = onClickDisputeTakeDown,
                        addSeparator = false,
                    )
                }
                if (showSlideShow) {
                    MenuActionListTile(
                        icon = R.drawable.ic_slideshow,
                        text = stringResource(id = R.string.action_slideshow),
                        onActionClicked = onClickSlideShow,
                        addSeparator = false,
                    )
                }
                MenuActionListTile(
                    icon = R.drawable.ic_open_with,
                    text = stringResource(id = R.string.external_play),
                    onActionClicked = onClickOpenWith,
                    addSeparator = true,
                )
                if (showForward) {
                    MenuActionListTile(
                        icon = R.drawable.ic_forward,
                        text = stringResource(id = R.string.forward_menu_item),
                        onActionClicked = onClickForward,
                        addSeparator = false,
                    )
                }
                MenuActionListTile(
                    icon = R.drawable.ic_save_to_device,
                    text = stringResource(id = R.string.general_save_to_device),
                    onActionClicked = onClickSaveToDevice,
                    addSeparator = false,
                )
                MenuActionListTile(
                    text = stringResource(id = R.string.file_properties_available_offline),
                    icon = R.drawable.ic_save_offline,
                    addSeparator = true,
                ) {
                    MegaSwitch(
                        checked = false,
                        onCheckedChange = onSwitchAvailableOffline,
                    )
                }
                MenuActionListTile(
                    icon = link_ic,
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals.get_links,
                        1,
                    ),
                    onActionClicked = onClickGetLink,
                    addSeparator = false,
                )
                if (showRemoveLink) {
                    MenuActionListTile(
                        icon = R.drawable.ic_remove_link,
                        text = stringResource(id = R.string.context_remove_link_menu),
                        onActionClicked = onClickRemoveLink,
                        addSeparator = false,
                    )
                }
                MenuActionListTile(
                    icon = R.drawable.ic_send_to_contact,
                    text = stringResource(id = R.string.context_send_file_to_chat),
                    onActionClicked = onClickSendTo,
                    addSeparator = false,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_social_share_white,
                    text = stringResource(id = R.string.general_share),
                    onActionClicked = onClickShare,
                    addSeparator = true,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_rename,
                    text = stringResource(id = R.string.context_rename),
                    onActionClicked = onClickRename,
                    addSeparator = false,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_move,
                    text = stringResource(id = R.string.general_move),
                    onActionClicked = onClickMove,
                    addSeparator = false,
                )
                MenuActionListTile(
                    icon = R.drawable.ic_menu_copy,
                    text = stringResource(id = R.string.context_copy),
                    onActionClicked = onClickCopy,
                    addSeparator = true,
                )
                if (showRestore) {
                    MenuActionListTile(
                        icon = R.drawable.ic_restore,
                        text = stringResource(id = R.string.context_restore),
                        onActionClicked = onClickRestore,
                        addSeparator = false,
                    )
                }
                if (showRemoveFromOffline) {
                    MenuActionListTile(
                        icon = R.drawable.ic_remove,
                        text = stringResource(id = R.string.context_delete_offline),
                        onActionClicked = onClickRemoveFromOffline,
                        addSeparator = false,
                    )
                }
                if (showRemove) {
                    MenuActionListTile(
                        icon = R.drawable.ic_remove,
                        text = stringResource(id = R.string.context_remove),
                        onActionClicked = onClickRemove,
                        addSeparator = false,
                    )
                }
                MenuActionListTile(
                    icon = R.drawable.ic_rubbish_bin,
                    text = stringResource(id = R.string.context_move_to_trash),
                    onActionClicked = onClickMoveToRubbishBin,
                    addSeparator = false,
                    isDestructive = true,
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