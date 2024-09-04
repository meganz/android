package mega.privacy.android.app.presentation.videosection.view.videoselected

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSelectedState
import mega.privacy.android.app.presentation.videosection.view.VideoSectionLoadingView
import mega.privacy.android.app.presentation.view.NodeGridView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_black
import mega.privacy.android.shared.original.core.ui.utils.ListGridStateMap
import mega.privacy.android.shared.original.core.ui.utils.getState
import mega.privacy.android.shared.original.core.ui.utils.sync

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun VideoSelectedView(
    uiState: VideoSelectedState,
    onItemClicked: (NodeUIItem<TypedNode>) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onVideoSelected: (List<Long>) -> Unit,
    onBackPressed: () -> Unit,
    onMenuActionClick: (FileInfoMenuAction) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
) {
    var listStateMap by rememberSaveable(saver = ListGridStateMap.Saver) {
        mutableStateOf(emptyMap())
    }

    /**
     * When back navigation performed from a folder, remove the listState/gridState of that node handle
     */
    LaunchedEffect(
        uiState.currentFolderHandle,
        uiState.nodesList,
        uiState.openedFolderNodeHandles
    ) {
        listStateMap =
            listStateMap.sync(uiState.openedFolderNodeHandles, uiState.currentFolderHandle)
    }

    Scaffold(
        modifier = modifier
            .systemBarsPadding()
            .semantics { testTagsAsResourceId = true },
        scaffoldState = rememberScaffoldState(),
        topBar = {
            VideoSelectedTopBar(
                title = uiState.topBarTitle
                    ?: stringResource(id = sharedR.string.video_section_video_selected_top_bar_title),
                searchState = uiState.searchState,
                query = uiState.query,
                isEmpty = uiState.nodesList.isEmpty(),
                selectedSize = uiState.selectedNodeHandles.size,
                onMenuActionClick = onMenuActionClick,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedNodeHandles.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = modifier
            ) {
                FloatingActionButton(
                    modifier = modifier.testTag(VIDEO_SELECTED_FAB_BUTTON_TEST_TAG),
                    onClick = {
                        onVideoSelected(uiState.selectedNodeHandles)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Create new video playlist",
                        tint = MaterialTheme.colors.white_black
                    )
                }
            }
        }
    ) { paddingValue ->
        when {
            uiState.isLoading -> VideoSectionLoadingView()

            uiState.nodesList.isEmpty() -> LegacyMegaEmptyViewWithImage(
                modifier = Modifier.testTag(VIDEO_SELECTED_EMPTY_VIEW_TEST_TAG),
                text = stringResource(id = R.string.homepage_empty_hint_video),
                imagePainter = painterResource(id = iconPackR.drawable.ic_video_section_empty_video)
            )

            else -> {
                val showSortOrder = true
                val showChangeViewType = true
                val showMediaDiscoveryButton = false
                val orientation = LocalConfiguration.current.orientation
                val span = if (orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 4
                val sortOrder = stringResource(
                    id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                        ?: R.string.sortby_name
                )
                val currentListState = listStateMap.getState(uiState.currentFolderHandle)

                if (uiState.currentViewType == ViewType.LIST) {
                    VideoSelectedNodeListView(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .testTag(VIDEO_SELECTED_LIST_VIEW_TEST_TAG),
                        listContentPadding = paddingValue,
                        nodeUIItemList = uiState.nodesList,
                        onItemClicked = onItemClicked,
                        sortOrder = sortOrder,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        showSortOrder = showSortOrder,
                        showChangeViewType = showChangeViewType,
                        listState = currentListState.lazyListState,
                        fileTypeIconMapper = fileTypeIconMapper
                    )
                } else {
                    val newList =
                        rememberNodeListForGrid(nodeUIItems = uiState.nodesList, spanCount = span)
                    NodeGridView(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .testTag(VIDEO_SELECTED_GRID_VIEW_TEST_TAG),
                        listContentPadding = paddingValue,
                        nodeUIItems = newList,
                        onMenuClick = {},
                        onItemClicked = onItemClicked,
                        onLongClick = {},
                        onEnterMediaDiscoveryClick = {},
                        spanCount = span,
                        sortOrder = sortOrder,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        showSortOrder = showSortOrder,
                        showChangeViewType = showChangeViewType,
                        gridState = currentListState.lazyGridState,
                        showMediaDiscoveryButton = showMediaDiscoveryButton,
                        isPublicNode = false,
                        fileTypeIconMapper = fileTypeIconMapper,
                    )
                }
            }
        }
    }
}

/**
 * Remember function for [NodeGridView] to form empty items in case of folders count are not as per
 * span count
 * @param nodeUIItems list of [NodeUIItem]
 * @param spanCount span count of [NodeGridView]
 */
@Composable
private fun <T : TypedNode> rememberNodeListForGrid(
    nodeUIItems: List<NodeUIItem<T>>,
    spanCount: Int,
) =
    remember(spanCount + nodeUIItems.hashCode()) {
        val folderCount = nodeUIItems.count {
            it.node is FolderNode
        }
        val placeholderCount =
            (folderCount % spanCount).takeIf { it != 0 }?.let { spanCount - it } ?: 0
        if (folderCount > 0 && placeholderCount > 0 && folderCount < nodeUIItems.size) {
            val gridItemList = nodeUIItems.toMutableList()
            repeat(placeholderCount) {
                val node = nodeUIItems[folderCount - 1].copy(
                    isInvisible = true,
                )
                gridItemList.add(folderCount, node)
            }
            return@remember gridItemList
        }
        nodeUIItems
    }

@CombinedThemePreviews
@Composable
private fun VideoSelectedViewWithProgressBarPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedView(
            uiState = VideoSelectedState(
                isLoading = true,
                topBarTitle = "Choose files",
            ),
            onSearchTextChange = {},
            onCloseClicked = {},
            onSearchClicked = {},
            onBackPressed = {},
            onVideoSelected = {},
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            onItemClicked = {},
            onMenuActionClick = {},
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoSelectedViewWithEmptyViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedView(
            uiState = VideoSelectedState(
                isLoading = false,
                nodesList = emptyList(),
                topBarTitle = "Choose files"
            ),
            onSearchTextChange = {},
            onCloseClicked = {},
            onSearchClicked = {},
            onBackPressed = {},
            onVideoSelected = {},
            onChangeViewTypeClick = {},
            onSortOrderClick = {},
            onItemClicked = {},
            onMenuActionClick = {},
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}

/**
 * Test tag for empty view
 */
const val VIDEO_SELECTED_EMPTY_VIEW_TEST_TAG = "video_selected:empty_view"

/**
 * Test tag for list view
 */
const val VIDEO_SELECTED_LIST_VIEW_TEST_TAG = "video_selected:list_view"

/**
 * Test tag for list view
 */
const val VIDEO_SELECTED_GRID_VIEW_TEST_TAG = "video_selected:grid_view"

/**
 * Test tag for fab button
 */
const val VIDEO_SELECTED_FAB_BUTTON_TEST_TAG = "video_selected:fab_button_videos_selected"

internal const val videoSelectedRoute = "videoSelectedFeature/videSelected"
