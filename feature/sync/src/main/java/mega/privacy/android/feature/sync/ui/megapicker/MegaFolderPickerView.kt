package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.skeleton.ListItemLoadingSkeleton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun MegaFolderPickerView(
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    nodesList: List<TypedNodeUiModel>?,
    sortOrder: String,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
    listState: LazyListState,
    onFolderClick: (TypedNode) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier) {
        when {
            nodesList == null || isLoading -> {
                item(key = "loading state") {
                    MegaFolderPickerViewLoadingState()
                }
            }

            nodesList.isEmpty() -> {
                item(key = "empty state") {
                    EmptyFolderPlaceHolder(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth()
                    )
                }
            }

            else -> {
                item(
                    key = "header"
                ) {
                    HeaderViewItem(
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        onEnterMediaDiscoveryClick = {},
                        sortOrder = sortOrder,
                        isListView = true,
                        showSortOrder = showSortOrder,
                        showChangeViewType = showChangeViewType
                    )
                }
                items(count = nodesList.size,
                    key = {
                        nodesList[it].node.id.longValue
                    }) {
                    val nodeEntity = nodesList[it].node

                    val icon = when (nodeEntity) {
                        is FolderNode -> nodeEntity.getIcon()
                        is FileNode -> fileTypeIconMapper(nodeEntity.type.extension)
                        else -> iconPackR.drawable.ic_generic_medium_solid
                    }
                    NodeListViewItem(
                        isSelected = false,
                        folderInfo = (nodeEntity as? FolderNode)
                            ?.folderInfo(),
                        icon = icon,
                        thumbnailData = ThumbnailRequest(nodeEntity.id),
                        fileSize = (nodeEntity as? FileNode)
                            ?.let { node -> formatFileSize(node.size, LocalContext.current) },
                        modifiedDate = (nodeEntity as? FileNode)
                            ?.let { node ->
                                formatModifiedDate(
                                    java.util.Locale(
                                        Locale.current.language, Locale.current.region
                                    ),
                                    node.modificationTime
                                )
                            },
                        name = nodeEntity.name,
                        showMenuButton = false,
                        isTakenDown = false,
                        isFavourite = false,
                        isSharedWithPublicLink = false,
                        onClick = { onFolderClick(nodeEntity) },
                        isEnabled = nodeEntity is FolderNode && nodesList[it].isDisabled.not(),
                    )
                    MegaDivider(dividerType = DividerType.FullSize)
                }
            }
        }
    }
}

/**
 * A Composable that displays the initial Loading state
 */
@Composable
private fun MegaFolderPickerViewLoadingState() {
    Column(
        modifier = Modifier.testTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE),
        content = {
            for (i in 1..20) {
                ListItemLoadingSkeleton()
            }
        }
    )
}

@Composable
private fun EmptyFolderPlaceHolder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(iconPackR.drawable.ic_empty_folder_glass),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .testTag(TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS)
        )
        MegaText(
            text = stringResource(R.string.sync_file_browser_empty_folder),
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

/**
 * A Preview Composable that displays the Loading Screen
 */
@Composable
@CombinedThemePreviews
private fun MegaFolderPickerViewLoadingStatePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaFolderPickerView(
            nodesList = emptyList(),
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = { },
            showSortOrder = true,
            listState = LazyListState(),
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {},
            fileTypeIconMapper = FileTypeIconMapper(),
            isLoading = false,
        )
    }
}

@Composable
@CombinedThemePreviews
private fun MegaFolderPickerViewEmptyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaFolderPickerView(
            nodesList = emptyList(),
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = { },
            showSortOrder = true,
            listState = LazyListState(),
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {},
            fileTypeIconMapper = FileTypeIconMapper(),
            isLoading = false,
        )
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewMegaFolderPickerView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaFolderPickerView(
            nodesList = SampleNodeDataProvider.values,
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = { },
            showSortOrder = true,
            listState = LazyListState(),
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {},
            fileTypeIconMapper = FileTypeIconMapper(),
            isLoading = false
        )
    }
}

@Composable
private fun FolderNode.folderInfo(): String {
    return if (childFolderCount == 0 && childFileCount == 0) {
        stringResource(R.string.sync_file_browser_empty_folder)
    } else if (childFolderCount == 0 && childFileCount > 0) {
        pluralStringResource(R.plurals.num_files_with_parameter, childFileCount, childFileCount)
    } else if (childFileCount == 0 && childFolderCount > 0) {
        pluralStringResource(
            R.plurals.num_folders_with_parameter,
            childFolderCount,
            childFolderCount
        )
    } else {
        pluralStringResource(
            R.plurals.num_folders_num_files,
            childFolderCount,
            childFolderCount
        ) + pluralStringResource(
            R.plurals.num_folders_num_files_2,
            childFileCount,
            childFileCount
        )
    }
}

internal const val TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE =
    "sync_mega_folder_picker_list_screen:loading_state"
internal const val TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS =
    "sync_mega_folder_picker_list_screen:no_items"
