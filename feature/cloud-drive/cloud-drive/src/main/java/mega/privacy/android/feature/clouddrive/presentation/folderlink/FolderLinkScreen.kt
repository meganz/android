package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.shared.nodes.components.NodesView
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.trackAnalyticsEvent
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.shared.nodes.components.NodeSkeletons
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderLinkScreen(
    viewModel: FolderLinkViewModel,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FolderLinkContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onBack = onBack,
        onAction = viewModel::processAction,
        onTransfer = onTransfer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderLinkContent(
    uiState: FolderLinkUiState,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onAction: (FolderLinkAction) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FOLDER_LINK_APP_BAR_TAG),
                title = uiState.title.text, // TODO update after finalized design
                navigationType = AppBarNavigationType.Back { onAction(FolderLinkAction.BackPressed) },
                trailingIcons = {
                    TransfersToolbarWidget {
                        onNavigate(TransfersNavKey())
                    }
                },
            )
        },
        bottomBar = {},
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding.excludingBottomPadding()),
        ) {
            when (val contentState = uiState.contentState) {
                FolderLinkContentState.Loading -> {
                    NodesViewSkeleton(
                        isListView = isListView,
                        spanCount = spanCount,
                        delay = NodeSkeletons.defaultDelay,
                    )
                }

                is FolderLinkContentState.DecryptionKeyRequired -> {
                    // TODO: show decryption key dialog in later MR
                    MegaText(text = if (contentState.isKeyIncorrect) "Invalid decryption key" else "Decryption key required")
                }

                FolderLinkContentState.Expired ->
                    MegaText(text = "This link has expired")

                FolderLinkContentState.Unavailable ->
                    MegaText(text = "This link is unavailable")

                is FolderLinkContentState.Loaded -> {
                    AnimatedContent(
                        targetState = uiState.currentFolderNode?.id,
                        transitionSpec = { fadeTransition },
                        label = "folder_nav_fade",
                    ) { parentNodeId ->
                        key(parentNodeId) {
                            val listState = rememberLazyListState()
                            val gridState = rememberLazyGridState()
                            NodesView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                listContentPadding = PaddingValues(
                                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                                ),
                                listState = listState,
                                gridState = gridState,
                                spanCount = spanCount,
                                items = contentState.items,
                                isNextPageLoading = false,
                                isHiddenNodesEnabled = false,
                                showHiddenNodes = true,
                                onMenuClicked = {
                                    onNavigate(
                                        NodeOptionsBottomSheetNavKey(
                                            nodeHandle = it.id.longValue,
                                            nodeSourceType = NodeSourceType.FOLDER_LINK,
                                        )
                                    )
                                },
                                onItemClicked = { onAction(FolderLinkAction.ItemClicked(it)) },
                                onLongClicked = {
                                    // TODO onAction(ItemLongClicked(it))
                                },
                                sortConfiguration = uiState.selectedSortConfiguration,
                                isListView = isListView,
                                onSortOrderClick = { showSortBottomSheet = true },
                                onChangeViewTypeClicked = {
                                    onAction(FolderLinkAction.ChangeViewTypeClicked)
                                },
                                inSelectionMode = false, // TODO
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.openedFileNode?.let { fileNode ->
        HandleNodeAction3(
            typedFileNode = fileNode,
            nodeSourceData = NodeSourceData.FolderLink,
            onNavigate = onNavigate,
            onActionHandled = { onAction(FolderLinkAction.OpenedFileNodeHandled) },
            onDownloadEvent = onTransfer,
            sortOrder = uiState.selectedSortOrder,
        )
    }

    if (showSortBottomSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.FOLDER_LINK),
            sheetState = sortBottomSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                sortDirection = uiState.selectedSortConfiguration.sortDirection,
            ),
            onSortOptionSelected = { result ->
                result?.let {
                    onAction(
                        FolderLinkAction.SortOrderChanged(
                            NodeSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection,
                            )
                        )
                    )
                    showSortBottomSheet = false
                    it.sortOptionItem.trackAnalyticsEvent()
                }
            },
            onDismissRequest = { showSortBottomSheet = false },
        )
    }

    BackHandler { onAction(FolderLinkAction.BackPressed) }

    EventEffect(
        event = uiState.navigateBackEvent,
        onConsumed = { onAction(FolderLinkAction.NavigateBackEventConsumed) },
        action = onBack,
    )
}


internal const val FOLDER_LINK_APP_BAR_TAG = "folder_link_screen:main_app_bar"
