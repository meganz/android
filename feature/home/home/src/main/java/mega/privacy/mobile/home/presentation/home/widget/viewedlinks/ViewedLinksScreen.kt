package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.flowOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.ViewedLinkGridLoadingItem
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.ViewedLinkListLoadingItem

/**
 * Full-screen Viewed Links list. Displays all viewed file and folder links
 * without the 4-item limit used in the Home widget. Items are loaded lazily
 * via Paging 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewedLinksScreen(
    uiState: ViewedLinksUiState,
    lazyItems: LazyPagingItems<ViewedLinkUiItem>,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    onClearAllLinks: () -> Unit,
    onSortOptionSelected: (NodeSortConfiguration) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onBack: () -> Unit,
) {
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val isListView = uiState.currentViewType == ViewType.LIST
    val isRefreshing = lazyItems.loadState.refresh is LoadState.Loading

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_widget_viewed_links_section_header),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(
                    MenuActionWithClick(CommonMenuAction.More) {
                        showOptionsSheet = true
                    }
                )
            )
        },
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        if (isListView) {
            ViewedLinksList(
                lazyItems = lazyItems,
                isRefreshing = isRefreshing,
                sortConfiguration = uiState.sortConfiguration,
                onSortOrderClick = { showSortSheet = true },
                onChangeViewTypeClick = onChangeViewTypeClick,
                onFolderLinkClicked = onFolderLinkClicked,
                onFileLinkClicked = onFileLinkClicked,
                modifier = modifier,
            )
        } else {
            ViewedLinksGrid(
                lazyItems = lazyItems,
                isRefreshing = isRefreshing,
                sortConfiguration = uiState.sortConfiguration,
                onSortOrderClick = { showSortSheet = true },
                onChangeViewTypeClick = onChangeViewTypeClick,
                onFolderLinkClicked = onFolderLinkClicked,
                onFileLinkClicked = onFileLinkClicked,
                modifier = modifier,
            )
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.CONTINUE_WHERE_LEFT_OFF),
            sheetState = sortSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.sortConfiguration.sortOption,
                sortDirection = uiState.sortConfiguration.sortDirection,
            ),
            onDismissRequest = { showSortSheet = false },
            onSortOptionSelected = { result ->
                if (result != null) {
                    onSortOptionSelected(
                        NodeSortConfiguration(
                            sortOption = result.sortOptionItem,
                            sortDirection = result.sortDirection,
                        )
                    )
                }
                showSortSheet = false
            },
        )
    }

    if (showOptionsSheet) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = optionsSheetState,
            onDismissRequest = { showOptionsSheet = false },
        ) {
            NodeActionListTile(
                text = stringResource(sharedR.string.home_widget_viewed_links_clear_history),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eraser),
                onActionClicked = {
                    showOptionsSheet = false
                    showClearConfirmationDialog = true
                },
                modifier = Modifier.testTag(CLEAR_HISTORY_TAG),
            )
        }
    }

    if (showClearConfirmationDialog) {
        BasicDialog(
            modifier = Modifier.testTag(CLEAR_HISTORY_DIALOG_TAG),
            title = stringResource(sharedR.string.home_widget_viewed_links_clear_history),
            description = stringResource(
                sharedR.string.home_widget_viewed_links_clear_history_dialog_message
            ),
            positiveButtonText = stringResource(sharedR.string.general_clear),
            negativeButtonText = stringResource(sharedR.string.general_dismiss_dialog),
            onPositiveButtonClicked = {
                showClearConfirmationDialog = false
                onClearAllLinks()
            },
            onDismiss = { showClearConfirmationDialog = false },
            onNegativeButtonClicked = { showClearConfirmationDialog = false },
        )
    }
}

@Composable
private fun ViewedLinksList(
    lazyItems: LazyPagingItems<ViewedLinkUiItem>,
    isRefreshing: Boolean,
    sortConfiguration: NodeSortConfiguration,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(key = SORT_HEADER_KEY) {
            SortHeader(
                sortConfiguration = sortConfiguration,
                isListView = true,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (isRefreshing) {
            items(count = LOADING_PLACEHOLDER_COUNT) { ViewedLinkListLoadingItem() }
        } else {
            items(
                count = lazyItems.itemCount,
                key = lazyItems.itemKey { it.viewedLink.nodeHandle },
            ) { index ->
                val item = lazyItems[index] ?: return@items
                ViewedLinkListItem(
                    item = item,
                    onFolderLinkClicked = onFolderLinkClicked,
                    onFileLinkClicked = onFileLinkClicked,
                    modifier = Modifier.animateItem(),
                )
            }
            if (lazyItems.loadState.append is LoadState.Loading) {
                item { ViewedLinkListLoadingItem() }
            }
        }
    }
}

@Composable
private fun ViewedLinksGrid(
    lazyItems: LazyPagingItems<ViewedLinkUiItem>,
    isRefreshing: Boolean,
    sortConfiguration: NodeSortConfiguration,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(
            key = SORT_HEADER_KEY,
            span = { GridItemSpan(maxLineSpan) },
        ) {
            SortHeader(
                sortConfiguration = sortConfiguration,
                isListView = false,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
            )
        }

        if (isRefreshing) {
            items(count = LOADING_PLACEHOLDER_COUNT) { ViewedLinkGridLoadingItem() }
        } else {
            items(
                count = lazyItems.itemCount,
                key = lazyItems.itemKey { it.viewedLink.nodeHandle },
            ) { index ->
                val item = lazyItems[index] ?: return@items
                ViewedLinkGridItem(
                    item = item,
                    onFolderLinkClicked = onFolderLinkClicked,
                    onFileLinkClicked = onFileLinkClicked,
                )
            }
            if (lazyItems.loadState.append is LoadState.Loading) {
                items(count = 2) { ViewedLinkGridLoadingItem() }
            }
        }
    }
}

@Composable
private fun SortHeader(
    sortConfiguration: NodeSortConfiguration,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NodeHeaderItem(
        onSortOrderClick = onSortOrderClick,
        onChangeViewTypeClick = onChangeViewTypeClick,
        sortConfiguration = sortConfiguration,
        isListView = isListView,
        showSortOrder = true,
        showChangeViewType = true,
        modifier = modifier,
    )
}

@Composable
private fun ViewedLinkListItem(
    item: ViewedLinkUiItem,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OneLineListItem(
        modifier = modifier
            .fillMaxWidth()
            .testTag(VIEWED_LINKS_ITEM_TEST_TAG),
        text = item.viewedLink.name,
        leadingElement = {
            NodeThumbnailView(
                modifier = Modifier.size(32.dp),
                layoutType = ThumbnailLayoutType.List,
                data = item.previewPath?.let { ThumbnailUriRequest(UriPath(it)) },
                defaultImage = item.iconRes,
                contentDescription = "Thumbnail",
            )
        },
        onClickListener = {
            when (item.viewedLink.type) {
                RecentlyViewedLinkType.FolderLink ->
                    onFolderLinkClicked(item.viewedLink.linkUrl)

                else ->
                    onFileLinkClicked(item.viewedLink.linkUrl)
            }
        },
    )
}

@Composable
private fun ViewedLinkGridItem(
    item: ViewedLinkUiItem,
    onFolderLinkClicked: (String) -> Unit,
    onFileLinkClicked: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(VIEWED_LINKS_GRID_ITEM_TEST_TAG)
            .clickable {
                when (item.viewedLink.type) {
                    RecentlyViewedLinkType.FolderLink ->
                        onFolderLinkClicked(item.viewedLink.linkUrl)

                    else ->
                        onFileLinkClicked(item.viewedLink.linkUrl)
                }
            },
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            NodeThumbnailView(
                modifier = Modifier
                    .align(Alignment.Center),
                layoutType = ThumbnailLayoutType.Grid,
                data = item.previewPath?.let { ThumbnailUriRequest(UriPath(it)) },
                defaultImage = item.iconRes,
                contentDescription = item.viewedLink.name,
                contentScale = ContentScale.Crop,
            )
        }
        MegaText(
            text = item.viewedLink.name,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

internal const val CLEAR_HISTORY_TAG = "viewed_links:clear_history"
internal const val CLEAR_HISTORY_DIALOG_TAG = "viewed_links:clear_history_dialog"
internal const val VIEWED_LINKS_ITEM_TEST_TAG = "viewed_links:item"
internal const val VIEWED_LINKS_GRID_ITEM_TEST_TAG = "viewed_links:grid_item"
internal const val SORT_HEADER_KEY = "viewed_links:sort_header"
private const val LOADING_PLACEHOLDER_COUNT = 6

@CombinedThemePreviews
@Composable
private fun ViewedLinksScreenPreview() {
    val previewItems = listOf(
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 1L,
                name = "Galicia 004.mov",
                linkUrl = "https://mega.nz/file/abc",
                type = RecentlyViewedLinkType.FileLink,
            ),
            iconRes = iconPackR.drawable.ic_video_medium_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 2L,
                name = "Galicia 005.mov",
                linkUrl = "https://mega.nz/file/def",
                type = RecentlyViewedLinkType.FileLink,
            ),
            iconRes = iconPackR.drawable.ic_video_medium_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 3L,
                name = "Susan Abulhawa notes.txt",
                linkUrl = "https://mega.nz/file/ghi",
                type = RecentlyViewedLinkType.FileLink,
            ),
            iconRes = iconPackR.drawable.ic_text_medium_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 4L,
                name = "Anne Carson - Gloves on article.pdf",
                linkUrl = "https://mega.nz/file/jkl",
                type = RecentlyViewedLinkType.FileLink,
            ),
            iconRes = iconPackR.drawable.ic_pdf_medium_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 5L,
                name = "Annemarie_Jacir",
                linkUrl = "https://mega.nz/folder/mno",
                type = RecentlyViewedLinkType.FolderLink,
            ),
            iconRes = iconPackR.drawable.ic_folder_users_small_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 6L,
                name = "Recipes",
                linkUrl = "https://mega.nz/folder/pqr",
                type = RecentlyViewedLinkType.FolderLink,
            ),
            iconRes = iconPackR.drawable.ic_folder_users_small_solid,
            previewPath = null,
        ),
        ViewedLinkUiItem(
            viewedLink = ViewedLink(
                nodeHandle = 7L,
                name = "Nabulus_soap_company_products.pdf",
                linkUrl = "https://mega.nz/file/stu",
                type = RecentlyViewedLinkType.FileLink,
            ),
            iconRes = iconPackR.drawable.ic_pdf_medium_solid,
            previewPath = null,
        ),
    )
    val lazyItems = flowOf(PagingData.from(previewItems)).collectAsLazyPagingItems()
    AndroidThemeForPreviews {
        ViewedLinksScreen(
            uiState = ViewedLinksUiState(),
            lazyItems = lazyItems,
            onFolderLinkClicked = {},
            onFileLinkClicked = {},
            onClearAllLinks = {},
            onSortOptionSelected = {},
            onChangeViewTypeClick = {},
            onBack = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ViewedLinksScreenLoadingPreview() {
    val lazyItems = flowOf<PagingData<ViewedLinkUiItem>>().collectAsLazyPagingItems()
    AndroidThemeForPreviews {
        ViewedLinksScreen(
            uiState = ViewedLinksUiState(),
            lazyItems = lazyItems,
            onFolderLinkClicked = {},
            onFileLinkClicked = {},
            onClearAllLinks = {},
            onSortOptionSelected = {},
            onChangeViewTypeClick = {},
            onBack = {},
        )
    }
}
