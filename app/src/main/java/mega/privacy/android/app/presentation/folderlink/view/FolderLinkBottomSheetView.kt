package mega.privacy.android.app.presentation.folderlink.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.R as CoreUiR
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FolderLinkBottomSheetView(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    nodeUIItem: NodeUIItem<TypedNode>?,
    showImport: Boolean,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onSaveToDeviceClicked: (NodeUIItem<TypedNode>?) -> Unit,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
) {
    val imageState = nodeUIItem?.let {
        produceState<File?>(initialValue = null) {
            getThumbnail(it.id.longValue) { file ->
                value = file
            }
        }
    }
    val fileIcon = MimeTypeList.typeForName(nodeUIItem?.node?.name).iconResourceId
    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        scrimColor = black.copy(alpha = 0.32f),
        sheetContent = {
            BottomSheetContent(
                modalSheetState = modalSheetState,
                coroutineScope = coroutineScope,
                nodeUIItem = nodeUIItem,
                showImport = showImport,
                onImportClicked = onImportClicked,
                onSaveToDeviceClicked = onSaveToDeviceClicked,
                imageState = imageState,
                icon = fileIcon
            )
        }
    ) {}
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    nodeUIItem: NodeUIItem<TypedNode>?,
    showImport: Boolean,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onSaveToDeviceClicked: (NodeUIItem<TypedNode>?) -> Unit,
    imageState: State<File?>?,
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
                    imageFile = imageState?.value,
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
                    painter = painterResource(id = CoreUiR.drawable.ic_folder_list),
                    contentDescription = "Image"
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = nodeUIItem?.name ?: "",
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Divider(
            modifier = Modifier.padding(start = 16.dp),
            color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
            thickness = 1.dp
        )
        MenuItem(
            modifier = Modifier.testTag(Constants.BOTTOM_SHEET_SAVE),
            res = R.drawable.ic_save_to_device,
            text = R.string.general_save_to_device,
            description = "Save",
            onClick = {
                coroutineScope.launch { modalSheetState.hide() }
                onSaveToDeviceClicked(nodeUIItem)
            }
        )
        if (showImport) {
            MenuItem(
                modifier = Modifier.testTag(Constants.BOTTOM_SHEET_IMPORT),
                res = R.drawable.ic_import_to_cloud_white,
                text = R.string.add_to_cloud,
                description = "Import",
                onClick = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onImportClicked(nodeUIItem)
                }
            )
        }
    }
}

@Composable
private fun MenuItem(
    modifier: Modifier,
    @DrawableRes res: Int,
    @StringRes text: Int,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = res),
            contentDescription = description,
            tint = MaterialTheme.colors.textColorSecondary
        )
        Text(
            modifier = Modifier
                .padding(horizontal = 36.dp, vertical = 2.dp)
                .align(Alignment.CenterVertically),
            text = stringResource(id = text),
            style = MaterialTheme.typography.subtitle1
        )
    }
}