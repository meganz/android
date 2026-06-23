package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.components.ExplorerNodeGridItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.ExplorerNodeListItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.explorerNodeClick
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerPickerRestrictions
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.rememberVisibleItems
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.DiscardScanWarningDialogNavKey
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.navigation.destination.UploadScannedDocumentNavKey
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@Composable
internal fun NodesExplorerScreen(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    nodeExplorerId: NodeId,
    nodeSourceType: NodeSourceType,
    isProcessingAction: Boolean,
    onCloseExplorerScreen: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    fileUriEvent: StateEventWithContent<UriPath>,
    shareUris: List<UriPath>? = null,
    onStartUpload: (TransferTriggerEvent) -> Unit = {},
    onFileUriConsumed: () -> Unit = {},
    onSelectFolder: (NodeId) -> Unit = {},
    onFilesPicked: (List<NodeId>) -> Unit = {},
    disabledTargetId: NodeId? = null,
    disabledNodeIds: Set<NodeId> = emptySet(),
    pickerRestrictions: ExplorerPickerRestrictions? = null,
    monitorResult: (String) -> Flow<Any?> = { emptyFlow() },
    clearResult: (String) -> Unit = {},
) {
    val uploadUrisEventState = rememberUploadUrisEventState()
    var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
    val folderPickedId = NodeId(folderPickedIdLong)
    val isUploading = explorerMode == ExplorerMode.ShareFilesToMega
            || explorerMode == ExplorerMode.ShareTextToMega
            || explorerMode == ExplorerMode.ShareURLToMega
            || explorerMode == ExplorerMode.SaveScannedDocument

    EventEffect(
        event = fileUriEvent,
        onConsumed = onFileUriConsumed,
    ) { uri ->
        uploadUrisEventState.trigger(listOf(uri.toUri()))
    }

    val onCancelExplorerScreen: () -> Unit = if (explorerMode == ExplorerMode.SaveScannedDocument
        && startNavKey is UploadScannedDocumentNavKey
    ) {
        {
            onNavigate(
                DiscardScanWarningDialogNavKey(
                    hasMultipleScans = startNavKey.hasMultipleScans,
                    startNavKey = startNavKey,
                )
            )
        }
    } else onCloseExplorerScreen

    ExplorerScreen(
        explorerMode = explorerMode,
        startNavKey = startNavKey,
        isInnerNavigation = true,
        nodeExplorerId = nodeExplorerId,
        nodeSourceType = nodeSourceType,
        shareUris = shareUris,
        disabledTargetId = disabledTargetId,
        disabledNodeIds = disabledNodeIds,
        pickerRestrictions = pickerRestrictions,
        onCloseExplorerScreen = onCancelExplorerScreen,
        isProcessingAction = isProcessingAction,
        onFolderPicked = { nodeId ->
            when {
                (explorerMode == ExplorerMode.ShareFilesToMega
                        || explorerMode == ExplorerMode.SaveScannedDocument)
                        && shareUris != null -> {
                    folderPickedIdLong = nodeId.longValue
                    uploadUrisEventState.trigger(
                        shareUris.map { it.toUri() }
                    )
                }

                explorerMode == ExplorerMode.ShareTextToMega
                        || explorerMode == ExplorerMode.ShareURLToMega -> {
                    folderPickedIdLong = nodeId.longValue
                    onNavigate(
                        if (explorerMode == ExplorerMode.ShareURLToMega) {
                            NewURLFileDialogNavKey(parentNodeId = nodeId)
                        } else {
                            NewTextFileDialogNavKey(
                                parentNodeId = nodeId,
                                returnFileName = true,
                            )
                        }
                    )
                }

                explorerMode == ExplorerMode.SelectCUFolder
                        || explorerMode == ExplorerMode.Copy
                        || explorerMode == ExplorerMode.Move
                        || explorerMode == ExplorerMode.Import
                        || explorerMode == ExplorerMode.AlbumImport -> {
                    onSelectFolder(nodeId)
                    onCloseExplorerScreen()
                }

                explorerMode == ExplorerMode.SelectSyncFolder
                        || explorerMode == ExplorerMode.SelectStopBackupDestination -> {
                    onSelectFolder(nodeId)
                }

                else -> {}
            }
        },
        onFilesPicked = { nodeIds ->
            onFilesPicked(nodeIds)
            onCloseExplorerScreen()
        },
        onNavigateBack = onNavigateBack,
        onNavigate = onNavigate,
        monitorResult = monitorResult,
        clearResult = clearResult,
    )

    if (isUploading) {
        ShareToMegaUpload(
            parentNodeId = folderPickedId,
            pitagTrigger = if (explorerMode == ExplorerMode.SaveScannedDocument) {
                PitagTrigger.Scanner
            } else {
                PitagTrigger.ShareFromApp
            },
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onCloseExplorerScreen,
            onNavigate = onNavigate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodesExplorerScreenContent(
    uiState: NodeExplorerUiState,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
    selectionState: NodeSelectionState = rememberNodeSelectionState(),
    isSelectionModeEnabled: Boolean = false,
    disabledNodeIds: Set<NodeId> = emptySet(),
    videosOnly: Boolean = false,
    restrictedNodeIds: Set<NodeId> = emptySet(),
    onRestrictedNodeClick: (NodeId) -> Unit = {},
    emptyView: @Composable () -> Unit = {
        if (uiState is NodeExplorerUiState.Data && !uiState.isRoot) EmptyFolder() else EmptyRoot()
    },
) {
    when (uiState) {
        NodeExplorerUiState.Loading -> NodesViewSkeleton()
        is NodeExplorerUiState.Data -> {
            val visibleItems = rememberVisibleItems(
                items = uiState.items,
                showHiddenNodes = uiState.showHiddenNodes,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
            )
            val onItemClicked = explorerNodeClick(
                selectionState = selectionState,
                disabledNodeIds = disabledNodeIds,
                videosOnly = videosOnly,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onFolderClick = onFolderClick,
                restrictedNodeIds = restrictedNodeIds,
                onRestrictedNodeClick = onRestrictedNodeClick,
            )

            EventEffect(
                event = uiState.navigateBack,
                onConsumed = consumeNavigateBack,
            ) { onNavigateBack() }

            NodeViewWithHeader(
                items = visibleItems,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                nodesLoadingState = uiState.nodesLoadingState,
                emptyView = emptyView,
                itemListView = {
                    ExplorerNodeListItem(
                        item = it,
                        isSelected = selectionState.selectedNodeIds.contains(it.id),
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                        videosOnly = videosOnly,
                        disabledNodeIds = disabledNodeIds,
                        restrictedNodeIds = restrictedNodeIds,
                        showLink = it.showLink,
                        onItemClicked = { onItemClicked(it) },
                    )
                },
                itemGridView = {
                    ExplorerNodeGridItem(
                        item = it,
                        isSelected = selectionState.selectedNodeIds.contains(it.id),
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                        videosOnly = videosOnly,
                        disabledNodeIds = disabledNodeIds,
                        restrictedNodeIds = restrictedNodeIds,
                        showLink = it.showLink,
                        onItemClicked = { onItemClicked(it) },
                    )
                },
                onRefreshNodes = onRefreshNodes,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun EmptyFolder() {
    EmptyStateView(
        title = stringResource(sharedR.string.context_empty_folder_title),
        imagePainter = painterResource(iconPackR.drawable.ic_empty_folder),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@Composable
private fun EmptyRoot() {
    EmptyStateView(
        title = stringResource(sharedR.string.context_empty_cloud_drive_title),
        imagePainter = painterResource(iconPackR.drawable.ic_usp_2),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@Composable
@CombinedThemePreviews
private fun NodesExplorerScreenContentEmptyPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreenContent(
                uiState = previewNodeExplorerData(),
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun NodesExplorerScreenContentPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreenContent(
                uiState = previewNodeExplorerData(
                    items = (1..4L).map { previewFolderNodeUiItem(it) }
                            + (10..15L).map { previewFileNodeUiItem(it) }
                ),
                isSelectionModeEnabled = true,
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

/** Builds a fully-loaded [NodeExplorerUiState.Data] for previews across the explorer screens. */
internal fun previewNodeExplorerData(
    items: List<NodeViewItem<TypedNode>> = emptyList(),
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) = NodeExplorerUiState.Data(
    currentFolderId = NodeId(-1),
    nodeSourceType = nodeSourceType,
    items = items,
    nodesLoadingState = NodesLoadingState.FullyLoaded,
    searchItems = emptyList(),
    searchLoadingState = NodesLoadingState.FullyLoaded,
    searchedQuery = null,
    showHiddenNodes = false,
    isHiddenNodesEnabled = false,
    isStorageOverQuota = false,
    navigateBack = consumed,
    folderName = LocalizedText.Literal(""),
    isRoot = true,
)

internal const val NODES_EXPLORER_VIEW_TAG = "nodes_explorer_view"
internal const val NODES_EXPLORER_EMPTY_VIEW_TAG = "$NODES_EXPLORER_VIEW_TAG:empty_view"