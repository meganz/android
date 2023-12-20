@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
    showFavouriteMenu: suspend (ImageNode) -> Boolean,
    showLabelMenu: suspend (ImageNode) -> Boolean,
    showDisputeMenu: suspend (ImageNode) -> Boolean,
    showOpenWithMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showGetLinkMenu: suspend (ImageNode) -> Boolean,
    showSendToChatMenu: suspend (ImageNode) -> Boolean,
    showShareMenu: suspend (ImageNode) -> Boolean,
    showRenameMenu: suspend (ImageNode) -> Boolean,
    showMoveMenu: suspend (ImageNode) -> Boolean,
    showCopyMenu: suspend (ImageNode) -> Boolean,
    showRestoreMenu: suspend (ImageNode) -> Boolean,
    showRemoveMenu: suspend (ImageNode) -> Boolean,
    showRemoveOfflineMenu: suspend (ImageNode) -> Boolean,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
    onClickInfo: () -> Unit = {},
    onClickFavourite: () -> Unit = {},
    onClickLabel: () -> Unit = {},
    onClickDispute: () -> Unit = {},
    onClickOpenWith: () -> Unit = {},
    onClickForward: () -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onSwitchAvailableOffline: (checked: Boolean) -> Unit = {},
    onClickGetLink: () -> Unit = {},
    onClickRemoveLink: () -> Unit = {},
    onClickSendToChat: () -> Unit = {},
    onClickShare: () -> Unit = {},
    onClickRename: () -> Unit = {},
    onClickMove: () -> Unit = {},
    onClickCopy: () -> Unit = {},
    onClickRestore: () -> Unit = {},
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

            val isFavouriteMenuVisible by produceState(false, imageNode) {
                value = showFavouriteMenu(imageNode)
            }

            val isLabelMenuVisible by produceState(false, imageNode) {
                value = showLabelMenu(imageNode)
            }

            val isDisputeMenuVisible by produceState(false, imageNode) {
                value = showDisputeMenu(imageNode)
            }

            val isOpenWithMenuVisible by produceState(false, imageNode) {
                value = showOpenWithMenu(imageNode)
            }

            val isForwardMenuVisible by produceState(false, imageNode) {
                value = showForwardMenu(imageNode)
            }

            val isSaveToDeviceMenuVisible by produceState(false, imageNode) {
                value = showSaveToDeviceMenu(imageNode)
            }

            val isGetLinkMenuVisible by produceState(false, imageNode) {
                value = showGetLinkMenu(imageNode)
            }

            val isSendToChatMenuVisible by produceState(false, imageNode) {
                value = showSendToChatMenu(imageNode)
            }

            val isShareMenuVisible by produceState(false, imageNode) {
                value = showShareMenu(imageNode)
            }

            val isRenameMenuVisible by produceState(false, imageNode) {
                value = showRenameMenu(imageNode)
            }

            val isMoveMenuVisible by produceState(false, imageNode) {
                value = showMoveMenu(imageNode)
            }

            val isCopyMenuVisible by produceState(false, imageNode) {
                value = showCopyMenu(imageNode)
            }

            val isRestoreMenuVisible by produceState(false, imageNode) {
                value = showRestoreMenu(imageNode)
            }

            val isRemoveMenuVisible by produceState(false, imageNode) {
                value = showRemoveMenu(imageNode)
            }

            val isRemoveOfflineMenuVisible by produceState(false, imageNode) {
                value = showRemoveOfflineMenu(imageNode)
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                MenuActionListTile(
                    icon = painterResource(id = R.drawable.info_ic),
                    text = stringResource(id = R.string.general_info),
                    onActionClicked = onClickInfo,
                    addSeparator = false,
                )

                if (isFavouriteMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(
                            id = if (imageNode.isFavourite) {
                                R.drawable.ic_remove_favourite
                            } else {
                                R.drawable.ic_add_favourite
                            }
                        ),
                        text = if (imageNode.isFavourite) {
                            stringResource(id = R.string.file_properties_unfavourite)
                        } else {
                            stringResource(id = R.string.file_properties_favourite)
                        },
                        onActionClicked = onClickFavourite,
                        addSeparator = false,
                    )
                }

                if (isLabelMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_label),
                        text = stringResource(id = R.string.file_properties_label),
                        onActionClicked = onClickLabel,
                        addSeparator = false,
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
                                            ),
                                    )
                                }
                            }
                        },
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isDisputeMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_taken_down_bottom_sheet),
                        text = stringResource(id = R.string.dispute_takendown_file),
                        onActionClicked = onClickDispute,
                        addSeparator = false,
                    )
                }

                if (isOpenWithMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_open_with),
                        text = stringResource(id = R.string.external_play),
                        onActionClicked = onClickOpenWith,
                        addSeparator = false,
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isForwardMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_forward),
                        text = stringResource(id = R.string.forward_menu_item),
                        onActionClicked = onClickForward,
                        addSeparator = false,
                    )
                }

                if (isSaveToDeviceMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_save_to_device),
                        text = stringResource(id = R.string.general_save_to_device),
                        onActionClicked = onClickSaveToDevice,
                        addSeparator = false,
                    )
                }

                if (!isRemoveOfflineMenuVisible) {
                    MenuActionListTile(
                        text = stringResource(id = R.string.file_properties_available_offline),
                        icon = painterResource(id = R.drawable.ic_save_offline),
                        addSeparator = false,
                    ) {
                        MegaSwitch(
                            checked = isAvailableOffline,
                            onCheckedChange = onSwitchAvailableOffline,
                        )
                    }
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isGetLinkMenuVisible) {
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
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove_link),
                        text = stringResource(id = R.string.context_remove_link_menu),
                        onActionClicked = onClickRemoveLink,
                        addSeparator = false,
                    )
                }

                if (isSendToChatMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_send_to_contact),
                        text = stringResource(id = R.string.context_send_file_to_chat),
                        onActionClicked = onClickSendToChat,
                        addSeparator = false,
                    )
                }

                if (isShareMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_social_share_white),
                        text = stringResource(id = R.string.general_share),
                        onActionClicked = onClickShare,
                        addSeparator = false,
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isRenameMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_rename),
                        text = stringResource(id = R.string.context_rename),
                        onActionClicked = onClickRename,
                        addSeparator = false,
                    )
                }

                if (isMoveMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_move),
                        text = stringResource(id = R.string.general_move),
                        onActionClicked = onClickMove,
                        addSeparator = false,
                    )
                }

                if (isCopyMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_menu_copy),
                        text = stringResource(id = R.string.context_copy),
                        onActionClicked = onClickCopy,
                        addSeparator = false,
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isRestoreMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_restore),
                        text = stringResource(id = R.string.context_restore),
                        onActionClicked = onClickRestore,
                        addSeparator = false,
                    )
                }

                if (isRemoveMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove),
                        text = stringResource(id = R.string.context_remove),
                        onActionClicked = onClickRemove,
                        addSeparator = false,
                    )
                }

                if (isRemoveOfflineMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove),
                        text = stringResource(id = R.string.context_delete_offline),
                        onActionClicked = { onSwitchAvailableOffline(false) },
                        isDestructive = true,
                        addSeparator = false,
                    )
                }

                if (!isRemoveOfflineMenuVisible) {
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
private fun ImagePreviewMenuActionHeader(
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