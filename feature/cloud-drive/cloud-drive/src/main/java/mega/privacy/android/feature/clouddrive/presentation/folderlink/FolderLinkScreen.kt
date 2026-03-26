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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SaveToMegaMenuAction
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveEmptyView
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.trackAnalyticsEvent
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkShareAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.startShareIntent
import mega.privacy.android.feature.clouddrive.presentation.folderlink.view.DecryptionKeyDialog
import mega.privacy.android.feature.clouddrive.presentation.folderlink.view.ExpiredLinkView
import mega.privacy.android.feature.clouddrive.presentation.folderlink.view.UnavailableLinkView
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.components.NodeSkeletons
import mega.privacy.android.shared.nodes.components.NodesView
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderLinkScreen(
    viewModel: FolderLinkViewModel,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    navigationHandler: NavigationHandler,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val singleNodeActionHandler: SingleNodeActionHandler = rememberSingleNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler,
    )
    val megaNavigator = rememberMegaNavigator()
    val selectionModeActionHandler: MultiNodeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val isLoaded = uiState.contentState is FolderLinkContentState.Loaded

    LaunchedEffect(uiState.selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            NodeSourceType.FOLDER_LINK,
        )
    }

    EventEffect(
        event = nodeOptionsActionUiState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered,
        action = {
            viewModel.processAction(FolderLinkAction.DeselectAllItems)
        }
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedItemsCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = { viewModel.processAction(FolderLinkAction.SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(FolderLinkAction.DeselectAllItems) },
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier.testTag(FOLDER_LINK_APP_BAR_TAG),
                    title = uiState.title.text,
                    subtitle = uiState.subTitle?.text,
                    navigationType = if (uiState.isRootFolder) {
                        AppBarNavigationType.Close {
                            viewModel.processAction(FolderLinkAction.BackPressed)
                        }
                    } else {
                        AppBarNavigationType.Back {
                            viewModel.processAction(FolderLinkAction.BackPressed)
                        }
                    },
                    trailingIcons = {
                        TransfersToolbarWidget {
                            onNavigate(TransfersNavKey())
                        }
                    },
                    actions = buildList {
                        if (isLoaded && uiState.isRootFolder) {
                            add(MenuActionWithClick(FolderLinkShareAction) {
                                context.startShareIntent(
                                    link = uiState.url,
                                    title = uiState.title.get(context)
                                )
                            })
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeBottomBar(
                    availableActions = nodeOptionsActionUiState.availableActions,
                    visibleActions = nodeOptionsActionUiState.visibleActions,
                    visible = nodeOptionsActionUiState.visibleActions.isNotEmpty(),
                    multiNodeActionHandler = selectionModeActionHandler,
                    selectedNodes = uiState.selectedNodes,
                    isSelecting = false,
                )
            } else {
                uiState.currentFolderNode?.let { folderNode ->
                    if (isLoaded) {
                        InlineAnchoredButtonGroup(
                            modifier = Modifier
                                .testTag(FOLDER_LINK_BOTTOM_BAR_TAG),
                            primaryButtonText = stringResource(sharedR.string.node_option_save_to_mega),
                            primaryButtonLeadingIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudUpload),
                            onPrimaryButtonClick = {
                                singleNodeActionHandler(SaveToMegaMenuAction(), folderNode)
                            },
                            textOnlyButtonText = stringResource(sharedR.string.general_save_to_device),
                            onTextOnlyButtonClick = {
                                singleNodeActionHandler(DownloadMenuAction(), folderNode)
                            }
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        FolderLinkContent(
            uiState = uiState,
            isListView = isListView,
            spanCount = spanCount,
            onNavigate = onNavigate,
            onAction = viewModel::processAction,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding.excludingBottomPadding()),
            bottomPadding = contentPadding.calculateBottomPadding(),
            onShowSortBottomSheet = { showSortBottomSheet = true },
        )
    }

    uiState.openedFileNode?.let { fileNode ->
        HandleNodeAction3(
            typedFileNode = fileNode,
            nodeSourceData = NodeSourceData.FolderLink,
            onNavigate = onNavigate,
            onActionHandled = { viewModel.processAction(FolderLinkAction.OpenedFileNodeHandled) },
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
                    viewModel.processAction(
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

    BackHandler { viewModel.processAction(FolderLinkAction.BackPressed) }

    EventEffect(
        event = uiState.navigateBackEvent,
        onConsumed = { viewModel.processAction(FolderLinkAction.NavigateBackEventConsumed) },
        action = onBack,
    )
}

@Composable
internal fun FolderLinkContent(
    uiState: FolderLinkUiState,
    isListView: Boolean,
    spanCount: Int,
    onNavigate: (NavKey) -> Unit,
    onAction: (FolderLinkAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onShowSortBottomSheet: () -> Unit = {},
) {
    Column(modifier = modifier) {
        when (val contentState = uiState.contentState) {
            FolderLinkContentState.Loading -> {
                NodesViewSkeleton(
                    modifier = Modifier.testTag(FOLDER_LINK_LOADING_TAG),
                    isListView = isListView,
                    spanCount = spanCount,
                    delay = NodeSkeletons.defaultDelay,
                )
            }

            is FolderLinkContentState.DecryptionKeyRequired -> {
                DecryptionKeyDialog(
                    isKeyIncorrect = contentState.isKeyIncorrect,
                    onDecryptionKeyEntered = { key ->
                        onAction(FolderLinkAction.DecryptionKeyEntered(key))
                    },
                    onDismiss = {
                        onAction(FolderLinkAction.DecryptionKeyDialogDismissed)
                    },
                )
            }

            FolderLinkContentState.Expired ->
                ExpiredLinkView(
                    title = sharedR.string.folder_link_expired_title,
                    modifier = Modifier.testTag(FOLDER_LINK_EXPIRED_TAG),
                )

            FolderLinkContentState.Unavailable ->
                UnavailableLinkView(
                    title = sharedR.string.folder_link_unavailable_title,
                    subtitle = sharedR.string.general_link_unavailable_subtitle,
                    bulletPoints = listOf(
                        sharedR.string.folder_link_unavailable_deleted,
                        sharedR.string.folder_link_unavailable_disabled,
                        sharedR.string.general_link_unavailable_invalid_url,
                        sharedR.string.folder_link_unavailable_tos_violation,
                    ),
                    modifier = Modifier.testTag(FOLDER_LINK_UNAVAILABLE_TAG),
                )

            FolderLinkContentState.Loaded -> {
                AnimatedContent(
                    targetState = uiState.currentFolderNode?.id,
                    transitionSpec = { fadeTransition },
                    label = "folder_nav_fade",
                ) { parentNodeId ->
                    key(parentNodeId) {
                        val listState = rememberLazyListState()
                        val gridState = rememberLazyGridState()

                        if (uiState.items.isEmpty()) {
                            CloudDriveEmptyView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 56.dp),
                                isRootCloudDrive = false,
                                showAddItems = false,
                                onAddItemsClicked = {}
                            )
                        } else {
                            NodesView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                listContentPadding = PaddingValues(
                                    bottom = bottomPadding + 100.dp,
                                ),
                                listState = listState,
                                gridState = gridState,
                                spanCount = spanCount,
                                items = uiState.items,
                                isNextPageLoading = false,
                                isHiddenNodesEnabled = false,
                                showHiddenNodes = true,
                                onMenuClicked = {
                                    onNavigate(
                                        NodeOptionsBottomSheetNavKey(
                                            nodeHandle = it.id.longValue,
                                            nodeSourceType = NodeSourceType.FOLDER_LINK,
                                            partiallyExpand = false
                                        )
                                    )
                                },
                                onItemClicked = { onAction(FolderLinkAction.ItemClicked(it)) },
                                onLongClicked = { onAction(FolderLinkAction.ItemLongClicked(it)) },
                                sortConfiguration = uiState.selectedSortConfiguration,
                                isListView = isListView,
                                onSortOrderClick = onShowSortBottomSheet,
                                onChangeViewTypeClicked = {
                                    onAction(FolderLinkAction.ChangeViewTypeClicked)
                                },
                                inSelectionMode = uiState.isInSelectionMode,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal const val FOLDER_LINK_APP_BAR_TAG = "folder_link_screen:main_app_bar"
internal const val FOLDER_LINK_BOTTOM_BAR_TAG = "folder_link_screen:bottom_bar"
internal const val FOLDER_LINK_LOADING_TAG = "folder_link_screen:loading"
internal const val FOLDER_LINK_EXPIRED_TAG = "folder_link_screen:expired"
internal const val FOLDER_LINK_UNAVAILABLE_TAG = "folder_link_screen:unavailable"
