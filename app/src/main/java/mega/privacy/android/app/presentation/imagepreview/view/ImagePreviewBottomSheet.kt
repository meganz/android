@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.view.extension.fileInfo
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.shared.original.core.ui.theme.accent_050
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_070
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_070
import mega.privacy.android.shared.resources.R as SharedResources
import nz.mega.sdk.MegaNode

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ImagePreviewBottomSheet(
    modalSheetState: ModalBottomSheetState,
    imageNode: ImageNode,
    showInfoMenu: suspend (ImageNode) -> Boolean,
    showFavouriteMenu: suspend (ImageNode) -> Boolean,
    showLabelMenu: suspend (ImageNode) -> Boolean,
    showDisputeMenu: suspend (ImageNode) -> Boolean,
    showOpenWithMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showImportMenu: suspend (ImageNode) -> Boolean,
    showGetLinkMenu: suspend (ImageNode) -> Boolean,
    showSendToChatMenu: suspend (ImageNode) -> Boolean,
    showShareMenu: suspend (ImageNode) -> Boolean,
    showRenameMenu: suspend (ImageNode) -> Boolean,
    showHideMenu: suspend (ImageNode) -> Boolean,
    showUnhideMenu: suspend (ImageNode) -> Boolean,
    forceHideHiddenMenus: () -> Boolean,
    showMoveMenu: suspend (ImageNode) -> Boolean,
    showCopyMenu: suspend (ImageNode) -> Boolean,
    showRestoreMenu: suspend (ImageNode) -> Boolean,
    showRemoveMenu: suspend (ImageNode) -> Boolean,
    showAvailableOfflineMenu: suspend (ImageNode) -> Boolean,
    showRemoveOfflineMenu: suspend (ImageNode) -> Boolean,
    showMoveToRubbishBin: suspend (ImageNode) -> Boolean,
    showAddToAlbum: suspend (ImageNode) -> Boolean,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
    isAvailableOffline: Boolean = false,
    accountType: AccountType? = null,
    isBusinessAccountExpired: Boolean = false,
    isHiddenNodesEnabled: Boolean = false,
    isHiddenNodesOnboarded: Boolean? = null,
    onClickInfo: () -> Unit = {},
    onClickFavourite: () -> Unit = {},
    onClickLabel: () -> Unit = {},
    onClickDispute: () -> Unit = {},
    onClickOpenWith: () -> Unit = {},
    onClickForward: () -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onClickImport: () -> Unit = {},
    onSwitchAvailableOffline: (checked: Boolean) -> Unit = {},
    onClickGetLink: () -> Unit = {},
    onClickRemoveLink: () -> Unit = {},
    onClickSendToChat: () -> Unit = {},
    onClickShare: () -> Unit = {},
    onClickRename: () -> Unit = {},
    onClickHide: () -> Unit = {},
    onClickHideHelp: () -> Unit = {},
    onClickUnhide: () -> Unit = {},
    onClickMove: () -> Unit = {},
    onClickCopy: () -> Unit = {},
    onClickRestore: () -> Unit = {},
    onClickRemove: () -> Unit = {},
    onClickMoveToRubbishBin: () -> Unit = {},
    onClickAddToAlbum: () -> Unit = {},
) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colors.isLight

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

            val isInfoMenuVisible by produceState(false, imageNode) {
                value = showInfoMenu(imageNode)
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

            val isImportMenuVisible by produceState(false, imageNode) {
                value = showImportMenu(imageNode)
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

            val isHideMenuVisible by produceState(false, imageNode) {
                value = showHideMenu(imageNode)
            }

            val isUnhideMenuVisible by produceState(false, imageNode) {
                value = showUnhideMenu(imageNode)
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

            val isAvailableOfflineMenuVisible by produceState(false, imageNode) {
                value = showAvailableOfflineMenu(imageNode)
            }

            val isRemoveOfflineMenuVisible by produceState(false, imageNode) {
                value = showRemoveOfflineMenu(imageNode)
            }

            val isMoveToRubbishBinMenuVisible by produceState(false, imageNode) {
                value = showMoveToRubbishBin(imageNode)
            }

            val isAddToAlbumMenuVisible by produceState(false, imageNode) {
                value = showAddToAlbum(imageNode)
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (isInfoMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.AlertCircle),
                        text = stringResource(id = R.string.general_info),
                        onActionClicked = onClickInfo,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_INFO),
                    )
                }
                if (isFavouriteMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(
                            if (imageNode.isFavourite) {
                                IconPack.Medium.Thin.Outline.HeartBroken
                            } else {
                                IconPack.Medium.Thin.Outline.Heart
                            }
                        ),
                        text = if (imageNode.isFavourite) {
                            stringResource(id = R.string.file_properties_unfavourite)
                        } else {
                            stringResource(id = R.string.file_properties_favourite)
                        },
                        onActionClicked = onClickFavourite,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_FAVOURITE),
                    )
                }

                if (isLabelMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.TagSimple),
                        text = stringResource(id = R.string.file_properties_label),
                        onActionClicked = onClickLabel,
                        dividerType = null,
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
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_LABEL),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isDisputeMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.AlertTriangle),
                        text = stringResource(id = R.string.dispute_takendown_file),
                        onActionClicked = onClickDispute,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_DISPUTE),
                    )
                }

                if (isOpenWithMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
                        text = stringResource(id = R.string.external_play),
                        onActionClicked = onClickOpenWith,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_OPEN_WITH),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isForwardMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.CornerUpRight),
                        text = stringResource(id = R.string.forward_menu_item),
                        onActionClicked = onClickForward,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_FORWARD),
                    )
                }

                if (isSaveToDeviceMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download),
                        text = stringResource(id = R.string.general_save_to_device),
                        onActionClicked = onClickSaveToDevice,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SAVE_TO_DEVICE),
                    )
                }

                if (isImportMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudUpload),
                        text = stringResource(id = R.string.general_import),
                        onActionClicked = onClickImport,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_IMPORT),
                    )
                }

                if (isAvailableOfflineMenuVisible) {
                    MenuActionListTile(
                        text = stringResource(id = R.string.file_properties_available_offline),
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ArrowDownCircle),
                        dividerType = null,
                        modifier = Modifier.testTag(
                            IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_AVAILABLE_OFFLINE
                        ),
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
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01),
                        text = if (imageNode.exportedData != null) {
                            stringResource(id = R.string.edit_link_option)
                        } else {
                            LocalContext.current.resources.getQuantityString(
                                SharedResources.plurals.label_share_links,
                                1,
                            )
                        },
                        onActionClicked = onClickGetLink,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_GET_LINK),
                    )
                }

                if (isGetLinkMenuVisible && imageNode.exportedData != null) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.LinkOff01),
                        text = stringResource(id = R.string.context_remove_link_menu),
                        onActionClicked = onClickRemoveLink,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE_LINK),
                    )
                }

                if (isSendToChatMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp),
                        text = stringResource(id = R.string.context_send_file_to_chat),
                        onActionClicked = onClickSendToChat,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SEND_TO_CHAT),
                    )
                }

                if (isShareMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork),
                        text = stringResource(id = R.string.general_share),
                        onActionClicked = onClickShare,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SHARE),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isRenameMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2),
                        text = stringResource(id = R.string.context_rename),
                        onActionClicked = onClickRename,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_RENAME),
                    )
                }

                if (isHiddenNodesEnabled && !forceHideHiddenMenus() && accountType != null && (!accountType.isPaid || isBusinessAccountExpired || (isHideMenuVisible && isHiddenNodesOnboarded != null))) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff),
                        text = stringResource(id = R.string.general_hide_node),
                        onActionClicked = onClickHide,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_HIDE),
                        trailingItem = {
                            if (!accountType.isPaid || isBusinessAccountExpired) {
                                Text(
                                    text = stringResource(id = R.string.general_pro_only),
                                    color = accent_900.takeIf { isLight } ?: accent_050,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W400,
                                    style = MaterialTheme.typography.subtitle1,
                                )
                            } else {
                                Icon(
                                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onClickHideHelp() },
                                    tint = grey_alpha_070.takeIf { isLight } ?: white_alpha_070,
                                )
                            }
                        },
                    )
                }

                if (isHiddenNodesEnabled && !forceHideHiddenMenus() && accountType?.isPaid == true && !isBusinessAccountExpired && isUnhideMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye),
                        text = stringResource(id = R.string.general_unhide_node),
                        onActionClicked = onClickUnhide,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_UNHIDE),
                    )
                }

                if (isMoveMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Move),
                        text = stringResource(id = R.string.general_move),
                        onActionClicked = onClickMove,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_MOVE),
                    )
                }

                if (isAddToAlbumMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.RectangleStackPlus),
                        text = stringResource(id = SharedResources.string.album_add_to_image),
                        onActionClicked = onClickAddToAlbum,
                        dividerType = null,
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isCopyMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Copy01),
                        text = stringResource(id = R.string.context_copy),
                        onActionClicked = onClickCopy,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_COPY),
                    )
                }

                if (isRestoreMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.RotateCcw),
                        text = stringResource(id = R.string.context_restore),
                        onActionClicked = onClickRestore,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_RESTORE),
                    )
                }

                if (isRemoveMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                        text = stringResource(id = R.string.context_remove),
                        isDestructive = true,
                        onActionClicked = onClickRemove,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE),
                    )
                }

                if (isRemoveOfflineMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                        text = stringResource(id = R.string.context_delete_offline),
                        onActionClicked = { onSwitchAvailableOffline(false) },
                        isDestructive = true,
                        dividerType = null,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE_OFFLINE),
                    )
                }

                if (isMoveToRubbishBinMenuVisible) {
                    MenuActionListTile(
                        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash),
                        text = stringResource(id = R.string.context_move_to_trash),
                        onActionClicked = onClickMoveToRubbishBin,
                        dividerType = null,
                        isDestructive = true,
                        modifier = Modifier.testTag(
                            IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_MOVE_TO_RUBBISH_BIN
                        ),
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
    val imageThumbnailPath by produceState<String?>(null, imageNode) {
        downloadImage(imageNode).collectLatest { imageResult ->
            value = getImageThumbnailPath(imageResult)
        }
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
                color = TextColor.Primary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_HEADER_TEXT_NAME),
            )
            MiddleEllipsisText(
                text = imageNode.fileInfo(),
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_HEADER_TEXT_INFO),
            )
        }
    }
}