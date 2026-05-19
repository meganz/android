package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.flowOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
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
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view.ViewedLinkLoadingItem

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
    onBack: () -> Unit,
) {
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val isRefreshing = lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item(key = SORT_HEADER_KEY) {
                NodeHeaderItem(
                    onSortOrderClick = { showSortSheet = true },
                    onChangeViewTypeClick = {
                        // Todo: Grid view type implemented next
                    },
                    sortConfiguration = uiState.sortConfiguration,
                    isListView = true,
                    showSortOrder = true,
                    showChangeViewType = false,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            if (isRefreshing) {
                items(count = LOADING_PLACEHOLDER_COUNT) {
                    ViewedLinkLoadingItem()
                }
            } else {
                items(
                    count = lazyItems.itemCount,
                    key = lazyItems.itemKey { it.viewedLink.nodeHandle },
                ) { index ->
                    val item = lazyItems[index] ?: return@items
                    OneLineListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(VIEWED_LINKS_ITEM_TEST_TAG)
                            .animateItem(),
                        text = item.viewedLink.name,
                        leadingElement = {
                            NodeThumbnailView(
                                modifier = Modifier.size(32.dp),
                                layoutType = ThumbnailLayoutType.List,
                                data = item.previewPath?.let {
                                    ThumbnailUriRequest(UriPath(it))
                                },
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
                if (lazyItems.loadState.append is LoadState.Loading) {
                    item {
                        ViewedLinkLoadingItem()
                    }
                }
            }
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

internal const val CLEAR_HISTORY_TAG = "viewed_links:clear_history"
internal const val CLEAR_HISTORY_DIALOG_TAG = "viewed_links:clear_history_dialog"
internal const val VIEWED_LINKS_ITEM_TEST_TAG = "viewed_links:item"
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
            onBack = {},
        )
    }
}
