package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContinueWhereLeftOffListScreen(
    viewModel: ContinueWhereLeftOffListViewModel,
    onNavigate: (NavKey) -> Unit,
    transferHandler: TransferHandler,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedFileNode by remember { mutableStateOf<TypedFileNode?>(null) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isListView = uiState.currentViewType == ViewType.LIST
    val coroutineScope = rememberCoroutineScope()

    EventEffect(
        event = uiState.openNodeEvent,
        onConsumed = viewModel::onOpenNodeEventConsumed,
    ) { node ->
        openedFileNode = node
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_widget_continue_where_left_off),
                navigationType = AppBarNavigationType.Back { onBack() },
                actions = listOf(
                    MenuActionWithClick(CommonMenuAction.More) {
                        viewModel.showOptionsSheet()
                    }
                ),
            )
        },
    ) { paddingValues ->
        when {
            !uiState.isConnected -> {
                EmptyStateView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    imagePainter = painterResource(id = IconPackR.drawable.ic_no_cloud),
                    title = stringResource(sharedR.string.sync_no_network_state),
                )
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    LargeInfiniteSpinnerIndicator()
                }
            }

            uiState.items.isEmpty() -> {
                EmptyStateView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    imagePainter = painterResource(id = R.drawable.ic_cwlo_empty_state),
                    title = stringResource(sharedR.string.home_cwlo_empty_state),
                )
            }

            isListView -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    item(key = "sort_header") {
                        NodeHeaderItem(
                            onSortOrderClick = viewModel::showSortSheet,
                            onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                            sortConfiguration = uiState.sortConfiguration,
                            isListView = true,
                            showSortOrder = true,
                            showChangeViewType = true,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                        )
                    }
                    items(
                        items = uiState.items,
                        key = { it.nodeHandle },
                        contentType = { it.type },
                    ) { item ->
                        ContinueWhereLeftOffListItem(
                            title = item.title,
                            nodeHandle = item.nodeHandle,
                            icon = iconForType(item.type),
                            onItemClicked = { viewModel.onItemClicked(item.nodeHandle, item.type) },
                        )
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(
                        key = "sort_header",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        NodeHeaderItem(
                            onSortOrderClick = viewModel::showSortSheet,
                            onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                            sortConfiguration = uiState.sortConfiguration,
                            isListView = false,
                            showSortOrder = true,
                            showChangeViewType = true,
                        )
                    }
                    items(
                        items = uiState.items,
                        key = { it.nodeHandle },
                        contentType = { it.type },
                    ) { item ->
                        ContinueWhereLeftOffGridItem(
                            title = item.title,
                            nodeHandle = item.nodeHandle,
                            duration = item.duration,
                            icon = iconForType(item.type),
                            onItemClicked = { viewModel.onItemClicked(item.nodeHandle, item.type) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showSortSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.CONTINUE_WHERE_LEFT_OFF),
            sheetState = sortSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.sortConfiguration.sortOption,
                sortDirection = uiState.sortConfiguration.sortDirection,
            ),
            onDismissRequest = viewModel::dismissSortSheet,
            onSortOptionSelected = { result ->
                if (result != null) {
                    viewModel.updateSortConfiguration(
                        NodeSortConfiguration(
                            sortOption = result.sortOptionItem,
                            sortDirection = result.sortDirection,
                        )
                    )
                } else {
                    viewModel.dismissSortSheet()
                }
            },
        )
    }

    if (uiState.showOptionsSheet) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = optionsSheetState,
            onDismissRequest = viewModel::dismissOptionsSheet,
        ) {
            NodeActionListTile(
                text = stringResource(sharedR.string.home_cwlo_clear_history),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eraser),
                onActionClicked = viewModel::clearAll,
                modifier = Modifier.testTag(CLEAR_HISTORY_TAG),
            )
        }
    }

    openedFileNode?.let { node ->
        HandleNodeAction3(
            typedFileNode = node,
            onActionHandled = { openedFileNode = null },
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            onDownloadEvent = transferHandler::setTransferEvent,
            onNavigate = onNavigate,
            coroutineScope = coroutineScope,
        )
    }
}

@Composable
private fun ContinueWhereLeftOffListItem(
    title: String,
    nodeHandle: Long,
    @DrawableRes icon: Int,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NodeThumbnailView(
            data = ThumbnailRequest(NodeId(nodeHandle)),
            defaultImage = icon,
            contentDescription = title,
            layoutType = ThumbnailLayoutType.List,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        MegaText(
            text = title,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun ContinueWhereLeftOffGridItem(
    title: String,
    nodeHandle: Long,
    duration: String?,
    @DrawableRes icon: Int,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked() },
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            NodeThumbnailView(
                data = ThumbnailRequest(NodeId(nodeHandle)),
                defaultImage = icon,
                contentDescription = title,
                layoutType = ThumbnailLayoutType.Grid,
                modifier = Modifier.matchParentSize(),
            )
            if (!duration.isNullOrEmpty()) {
                DurationBadge(
                    duration = duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                )
            }
        }
        MegaText(
            text = title,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

internal const val CLEAR_HISTORY_TAG = "cwlo_list:clear_history"

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffListItemPreview() {
    AndroidThemeForPreviews {
        Column {
            ContinueWhereLeftOffListItem(
                title = "Falastin36_press_trailer.mov",
                nodeHandle = 1L,
                icon = IconPackR.drawable.ic_video_medium_solid,
                onItemClicked = {},
            )
            ContinueWhereLeftOffListItem(
                title = "Interview Agnes Varda.pdf",
                nodeHandle = 2L,
                icon = IconPackR.drawable.ic_pdf_medium_solid,
                onItemClicked = {},
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffGridItemPreview() {
    AndroidThemeForPreviews {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContinueWhereLeftOffGridItem(
                title = "Falastin36_press_trailer.mov",
                nodeHandle = 1L,
                duration = "1:34",
                icon = IconPackR.drawable.ic_video_medium_solid,
                onItemClicked = {},
                modifier = Modifier.width(140.dp),
            )
            ContinueWhereLeftOffGridItem(
                title = "Interview Agnes Varda.pdf",
                nodeHandle = 2L,
                duration = null,
                icon = IconPackR.drawable.ic_pdf_medium_solid,
                onItemClicked = {},
                modifier = Modifier.width(140.dp),
            )
        }
    }
}
