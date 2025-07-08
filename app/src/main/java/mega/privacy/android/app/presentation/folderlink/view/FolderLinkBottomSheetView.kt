package mega.privacy.android.app.presentation.folderlink.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText

@Composable
internal fun FolderLinkBottomSheetView(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    nodeUIItem: NodeUIItem<TypedNode>?,
    showImport: Boolean,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onSaveToDeviceClicked: (NodeUIItem<TypedNode>?) -> Unit,
    content: @Composable () -> Unit,
) {
    val fileIcon = MimeTypeList.typeForName(nodeUIItem?.node?.name).iconResourceId
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetBody = {
            BottomSheetContent(
                modalSheetState = modalSheetState,
                coroutineScope = coroutineScope,
                nodeUIItem = nodeUIItem,
                showImport = showImport,
                onImportClicked = onImportClicked,
                onSaveToDeviceClicked = onSaveToDeviceClicked,
                icon = fileIcon
            )
        },
        content = content
    )
}

@Composable
private fun BottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    nodeUIItem: NodeUIItem<TypedNode>?,
    showImport: Boolean,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onSaveToDeviceClicked: (NodeUIItem<TypedNode>?) -> Unit,
    icon: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val infoText = nodeUIItem?.let {
            if (it.node is FolderNode) {
                it.node.folderInfo()
            } else {
                val fileItem = it.node as FileNode
                formatFileSize(fileItem.size, LocalContext.current)
            }
        } ?: ""

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (nodeUIItem != null && nodeUIItem.node is FileNode) {
                ThumbnailView(
                    contentDescription = "Image",
                    data = ThumbnailRequest(id = nodeUIItem.node.id, isPublicNode = true),
                    defaultImage = icon,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
            } else {
                Image(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    painter = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
                    contentDescription = "Image"
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                MegaText(
                    text = nodeUIItem?.name ?: "",
                    textColor = TextColor.Primary,
                    overflow = LongTextBehaviour.Ellipsis(1)
                )
                MegaText(
                    text = infoText,
                    textColor = TextColor.Secondary,
                    overflow = LongTextBehaviour.Ellipsis(1)
                )
            }
        }
        MegaDivider(
            modifier = Modifier.fillMaxWidth(),
            dividerType = DividerType.SmallStartPadding,
            strong = true,
        )

        MenuActionListTile(
            modifier = Modifier.testTag(Constants.BOTTOM_SHEET_SAVE),
            icon = rememberVectorPainter(
                IconPack.Medium.Thin.Outline.Download),
            text = stringResource(R.string.general_save_to_device),
            onActionClicked = {
                coroutineScope.launch { modalSheetState.hide() }
                onSaveToDeviceClicked(nodeUIItem)
            }
        )
        if (showImport) {
            MenuActionListTile(
                modifier = Modifier.testTag(Constants.BOTTOM_SHEET_IMPORT),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudUpload),
                text = stringResource(R.string.add_to_cloud),
                onActionClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onImportClicked(nodeUIItem)
                }
            )
        }
    }
}