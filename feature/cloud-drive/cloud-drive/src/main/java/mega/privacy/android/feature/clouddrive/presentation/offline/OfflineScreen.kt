package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionIconWithClick
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.nodecomponents.components.offline.HandleOfflineNodeAction3
import mega.privacy.android.core.nodecomponents.components.offline.OfflineNodeActionsViewModel
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.list.NodeGridViewItem
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineSelectionAction
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * OfflineScreen - A purely composable screen for displaying offline files
 *
 * @param onBack Callback for back navigation
 * @param viewModel The OfflineViewModel to manage state
 * @param modifier Modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineViewModel = hiltViewModel(),
    actionViewModel: OfflineNodeActionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionUiState by actionViewModel.uiState.collectAsStateWithLifecycle()

    HandleOfflineNodeAction3(
        uiState = actionUiState,
        consumeShareFilesEvent = actionViewModel::onShareFilesEventConsumed,
        consumeShareNodeLinksEvent = actionViewModel::onShareNodeLinksEventConsumed,
        consumeOpenFileEvent = actionViewModel::onOpenFileEventConsumed
    )

    OfflineScreen(
        uiState = uiState,
        onBack = onBack,
        selectAll = viewModel::selectAll,
        deselectAll = viewModel::clearSelection,
        onItemClicked = viewModel::onItemClicked,
        onItemLongClicked = viewModel::onLongItemClicked,
        onOpenFile = actionViewModel::handleOpenOfflineFile,
        onDismissOfflineWarning = viewModel::dismissOfflineWarning,
        onNavigateToFolder = onNavigateToFolder,
        onSearch = viewModel::setSearchQuery,
        consumeOpenFolderEvent = viewModel::onOpenFolderInPageEventConsumed,
        consumeOpenFileEvent = viewModel::onOpenOfflineNodeEventConsumed,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfflineScreen(
    uiState: OfflineUiState,
    onBack: () -> Unit,
    selectAll: () -> Unit,
    deselectAll: () -> Unit,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onOpenFile: (OfflineFileInformation) -> Unit,
    onDismissOfflineWarning: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    consumeOpenFolderEvent: () -> Unit = {},
    consumeOpenFileEvent: () -> Unit = {},
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    EventEffect(
        event = uiState.openFolderInPageEvent,
        onConsumed = consumeOpenFolderEvent
    ) { folderNode ->
        onNavigateToFolder(folderNode.id, folderNode.name)
    }

    EventEffect(
        event = uiState.openOfflineNodeEvent,
        onConsumed = consumeOpenFileEvent
    ) { file ->
        onOpenFile(file)
    }

    BackHandler(uiState.selectedNodeHandles.isNotEmpty()) {
        deselectAll()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            SelectionModeBottomBar(
                visible = uiState.selectedNodeHandles.isNotEmpty(),
                actions = OfflineSelectionAction.bottomBarItems,
                onActionPressed = {
                    when (it) {
                        is OfflineSelectionAction.Delete -> {
                            // Todo implement delete action
                            deselectAll()
                        }

                        is OfflineSelectionAction.Download -> {
                            // Todo implement download action
                            deselectAll()
                        }

                        is OfflineSelectionAction.Share -> {
                            // Todo implement share action
                            deselectAll()
                        }
                    }
                }
            )
        },
        topBar = {
            if (uiState.selectedNodeHandles.isEmpty()) {
                MegaSearchTopAppBar(
                    navigationType = AppBarNavigationType.Back(onBack),
                    title = uiState
                        .title
                        .takeIf { uiState.nodeId != -1 }
                        ?: stringResource(R.string.offline_screen_title),
                    query = uiState.searchQuery,
                    onQueryChanged = onSearch,
                    isSearchingMode = isSearchMode,
                    onSearchingModeChanged = {
                        isSearchMode = it
                        if (!it) {
                            // Clear search query when exiting search mode and
                            // reset to the original search result
                            onSearch("")
                        }
                    },
                    actions = buildList {
                        if (uiState.nodeId != -1) {
                            add(
                                MenuActionIconWithClick(CloudDriveAppBarAction.More) {
                                    // Todo implement NodeOptionsBottomSheet
                                }
                            )
                        }
                    }
                )
            } else {
                MegaTopAppBar(
                    title = uiState.selectedNodeHandles.size.toString(),
                    navigationType = AppBarNavigationType.Close(deselectAll),
                    actions = OfflineSelectionAction.topBarItems,
                    onActionPressed = { action ->
                        when (action) {
                            is OfflineSelectionAction.SelectAll -> selectAll()
                            else -> return@MegaTopAppBar
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (uiState.showOfflineWarning && !uiState.isLoading) {
                TopWarningBanner(
                    modifier = Modifier.fillMaxWidth(),
                    body = stringResource(R.string.offline_warning),
                    showCancelButton = true,
                    onCancelButtonClick = onDismissOfflineWarning
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LargeHUD(
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }

                    uiState.offlineNodes.isEmpty() -> {
                        MegaEmptyView(
                            modifier = Modifier.align(Alignment.Center),
                            text = "No offline files available",
                            imagePainter = painterResource(iconPackR.drawable.ic_arrow_circle_down_glass)
                        )
                    }

                    else -> {
                        OfflineContent(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            onItemClicked = onItemClicked,
                            onItemLongClicked = onItemLongClicked,
                            onMoreClicked = {
                                // Todo implement NodeOptionsBottomSheet
                            },
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineContent(
    uiState: OfflineUiState,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onMoreClicked: (OfflineNodeUiItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }

    when (uiState.currentViewType) {
        ViewType.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = contentPadding
            ) {
                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeListViewItem(
                        title = node.offlineFileInformation.name,
                        subtitle = getOfflineNodeDescription(node.offlineFileInformation),
                        icon = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            val extension =
                                node.offlineFileInformation.name.substringAfterLast('.', "")
                                    .takeIf { it.isNotEmpty() }
                                    ?: return@items
                            fileTypeIconMapper(extension)
                        },
                        description = null,
                        tags = null,
                        thumbnailData = node.offlineFileInformation.thumbnail,
                        highlightText = uiState.searchQuery ?: "",
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        showIsVerified = false,
                        isTakenDown = false,
                        label = null,
                        showLink = false,
                        showFavourite = false,
                        isSensitive = false,
                        showBlurEffect = false,
                        isHighlighted = node.isHighlighted,
                        onMoreClicked = { onMoreClicked(node) },
                        onItemClicked = { onItemClicked(node) },
                        onLongClicked = { onItemLongClicked(node) }
                    )
                }
            }
        }

        ViewType.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = modifier,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeGridViewItem(
                        name = node.offlineFileInformation.name,
                        iconRes = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            val extension =
                                node.offlineFileInformation.name.substringAfterLast('.', "")
                                    .takeIf { it.isNotEmpty() }
                                    ?: return@items
                            fileTypeIconMapper(extension)
                        },
                        thumbnailData = node.offlineFileInformation.thumbnail,
                        isTakenDown = false,
                        duration = null,
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        isFolderNode = node.offlineFileInformation.isFolder,
                        isVideoNode = false, // TODO: Add video detection
                        highlightText = uiState.searchQuery ?: "",
                        isSensitive = false,
                        showBlurEffect = false,
                        isHighlighted = node.isHighlighted,
                        showLink = false,
                        showFavourite = false,
                        label = null,
                        onClick = { onItemClicked(node) },
                        onLongClick = { onItemLongClicked(node) },
                        onMenuClick = { onMoreClicked(node) }
                    )
                }
            }
        }
    }
}

@Composable
private fun getOfflineNodeDescription(offlineFileInformation: OfflineFileInformation): String {
    val context = LocalContext.current
    return if (offlineFileInformation.isFolder) {
        offlineFileInformation.folderInfo?.let { folderInfo ->
            if (folderInfo.numFolders == 0 && folderInfo.numFiles == 0) {
                stringResource(R.string.file_browser_empty_folder)
            } else if (folderInfo.numFolders == 0 && folderInfo.numFiles > 0) {
                pluralStringResource(
                    R.plurals.num_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            } else if (folderInfo.numFiles == 0 && folderInfo.numFolders > 0) {
                pluralStringResource(
                    R.plurals.num_folders_with_parameter,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                )
            } else {
                pluralStringResource(
                    R.plurals.num_folders_num_files,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                ) + pluralStringResource(
                    R.plurals.num_folders_num_files_2,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            }
        } ?: run {
            ""
        }
    } else {
        formatFileSize(offlineFileInformation.totalSize, context)
            .plus(
                offlineFileInformation
                    .addedTime
                    ?.let {
                        " · ".plus(
                            formatModifiedDate(
                                java.util.Locale(
                                    Locale.current.language, Locale.current.region
                                ),
                                it
                            )
                        )
                    } ?: "")
    }
}
