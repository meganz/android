@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.core.R.drawable.link_ic
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.legacy.core.ui.controls.text.MiddleEllipsisText
import nz.mega.sdk.MegaNode

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ImagePreviewBottomSheet(
    modalSheetState: ModalBottomSheetState,
    imageNode: ImageNode,
    isAvailableOffline: Boolean = false,
    showDisputeTakeDown: Boolean = false,
    showSlideShow: Boolean = false,
    showForward: Boolean = false,
    showRestore: Boolean = false,
    showRemoveFromOffline: Boolean = false,
    showRemove: Boolean = false,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
    onClickInfo: () -> Unit = {},
    onClickFavourite: () -> Unit = {},
    onClickLabel: () -> Unit = {},
    onClickDisputeTakeDown: () -> Unit = {},
    onClickSlideShow: () -> Unit = {},
    onClickOpenWith: () -> Unit = {},
    onClickForward: () -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onSwitchAvailableOffline: ((checked: Boolean) -> Unit)? = null,
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
) {
    val context = LocalContext.current

    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = {
            ImagePreviewMenuActionHeader(
                imageNode = imageNode,
                downloadImage = downloadImage,
                getImageThumbnailPath = getImageThumbnailPath,
            )
        },
        sheetBody = {
            val labelColorResId = remember(imageNode) {
                MegaNodeUtil.getNodeLabelColor(imageNode.label)
            }

            val labelColorText = remember(imageNode) {
                MegaNodeUtil.getNodeLabelText(imageNode.label, context)
            }

            LazyColumn {
                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.info_ic),
                        text = stringResource(id = R.string.general_info),
                        onActionClicked = onClickInfo,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(
                            id = if (imageNode.isFavourite) R.drawable.ic_remove_favourite
                            else R.drawable.ic_add_favourite
                        ),
                        text = if (imageNode.isFavourite) stringResource(id = R.string.file_properties_unfavourite)
                        else stringResource(id = R.string.file_properties_favourite),
                        onActionClicked = onClickFavourite,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(icon = painterResource(id = R.drawable.ic_label),
                        text = stringResource(id = R.string.file_properties_label),
                        onActionClicked = onClickLabel,
                        addSeparator = true,
                        trailingItem = {
                            if (imageNode.label != MegaNode.NODE_LBL_UNKNOWN) {
                                Row {
                                    Text(
                                        text = labelColorText,
                                        color = colorResource(id = labelColorResId),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 4.dp)
                                            .size(10.dp)
                                            .background(
                                                shape = CircleShape,
                                                color = colorResource(id = labelColorResId),
                                            )
                                    )
                                }
                            }
                        })
                }

                if (showDisputeTakeDown) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_taken_down_bottom_sheet),
                            text = stringResource(id = R.string.dispute_takendown_file),
                            onActionClicked = onClickDisputeTakeDown,
                            addSeparator = false,
                        )
                    }
                }

                if (showSlideShow) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_slideshow),
                            text = stringResource(id = R.string.action_slideshow),
                            onActionClicked = onClickSlideShow,
                            addSeparator = false,
                        )
                    }
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_open_with),
                        text = stringResource(id = R.string.external_play),
                        onActionClicked = onClickOpenWith,
                        addSeparator = true,
                    )
                }

                if (showForward) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_forward),
                            text = stringResource(id = R.string.forward_menu_item),
                            onActionClicked = onClickForward,
                            addSeparator = false,
                        )
                    }
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_save_to_device),
                        text = stringResource(id = R.string.general_save_to_device),
                        onActionClicked = onClickSaveToDevice,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(
                        text = stringResource(id = R.string.file_properties_available_offline),
                        icon = painterResource(id = R.drawable.ic_save_offline),
                        addSeparator = true,
                    ) {
                        MegaSwitch(
                            checked = isAvailableOffline,
                            onCheckedChange = onSwitchAvailableOffline,
                        )
                    }
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = link_ic),
                        text = if (imageNode.exportedData != null) {
                            stringResource(id = R.string.edit_link_option)
                        } else {
                            LocalContext.current.resources.getQuantityString(
                                R.plurals.get_links,
                                1,
                            )
                        },
                        onActionClicked = onClickGetLink,
                        addSeparator = false,
                    )
                }

                if (imageNode.exportedData != null) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_remove_link),
                            text = stringResource(id = R.string.context_remove_link_menu),
                            onActionClicked = onClickRemoveLink,
                            addSeparator = false,
                        )
                    }
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_send_to_contact),
                        text = stringResource(id = R.string.context_send_file_to_chat),
                        onActionClicked = onClickSendTo,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_social_share_white),
                        text = stringResource(id = R.string.general_share),
                        onActionClicked = onClickShare,
                        addSeparator = true,
                    )
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_rename),
                        text = stringResource(id = R.string.context_rename),
                        onActionClicked = onClickRename,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_move),
                        text = stringResource(id = R.string.general_move),
                        onActionClicked = onClickMove,
                        addSeparator = false,
                    )
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_menu_copy),
                        text = stringResource(id = R.string.context_copy),
                        onActionClicked = onClickCopy,
                        addSeparator = true,
                    )
                }

                if (showRestore) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_restore),
                            text = stringResource(id = R.string.context_restore),
                            onActionClicked = onClickRestore,
                            addSeparator = false,
                        )
                    }
                }

                if (showRemoveFromOffline) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_remove),
                            text = stringResource(id = R.string.context_delete_offline),
                            onActionClicked = onClickRemoveFromOffline,
                            addSeparator = false,
                        )
                    }
                }

                if (showRemove) {
                    item {
                        MenuActionListTile(
                            icon = painterResource(id = R.drawable.ic_remove),
                            text = stringResource(id = R.string.context_remove),
                            onActionClicked = onClickRemove,
                            addSeparator = false,
                        )
                    }
                }

                item {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_rubbish_bin),
                        text = stringResource(id = R.string.context_move_to_trash),
                        onActionClicked = onClickMoveToRubbishBin,
                        addSeparator = false,
                        isDestructive = true,
                    )
                }
            }
        },
    )
}

@Composable
internal fun ImagePreviewMenuActionHeader(
    imageNode: ImageNode,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
) {
    val context = LocalContext.current

    val imageThumbnailPath by produceState<String?>(null, imageNode) {
        downloadImage(imageNode).collectLatest { imageResult ->
            value = getImageThumbnailPath(imageResult)
        }
    }

    val imageInfo = remember(imageNode) {
        MegaNode.unserialize(imageNode.serializedData).getInfoText(context)
    }

    Row(
        modifier = Modifier.padding(all = 16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageThumbnailPath)
                .crossfade(true).build(),
            placeholder = painterResource(id = android.R.drawable.ic_menu_camera),
            error = painterResource(id = android.R.drawable.ic_menu_camera),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp, 40.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            MiddleEllipsisText(
                text = imageNode.name,
                color = MaterialTheme.colors.textColorPrimary,
            )
            MiddleEllipsisText(
                text = imageInfo,
                color = MaterialTheme.colors.textColorSecondary,
            )
        }
    }
}