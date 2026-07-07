package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.copy.CopyViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.move.MoveViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.picker.NodePickerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.picker.NodePickerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.picker.TargetNodePickerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaUiState
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.uploadscanneddocument.UploadScannedDocumentScreen
import mega.privacy.android.feature.cloudexplorer.presentation.uploadscanneddocument.UploadScannedDocumentsViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.AddVideoToPlaylistNavKey
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.CopyResult
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.ImportAlbumNavKey
import mega.privacy.android.navigation.destination.ImportNavKey
import mega.privacy.android.navigation.destination.MoveNavKey
import mega.privacy.android.navigation.destination.MoveResult
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.PinToHomeNavKey
import mega.privacy.android.navigation.destination.SelectCUFolderNavKey
import mega.privacy.android.navigation.destination.SelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.SelectSyncFolderNavKey
import mega.privacy.android.navigation.destination.ShareFilesToChatNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.navigation.destination.UploadScannedDocumentNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareFilesToMegaDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
                monitorResult = navigationHandler::monitorResult,
                clearResult = navigationHandler::clearResult,
            )
            shareTextToMegaDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
                monitorResult = navigationHandler::monitorResult,
                clearResult = navigationHandler::clearResult,
            )
            uploadScannedDocumentDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
            )
            val onSelectCUFolder: (NodeId) -> Unit = { nodeId ->
                navigationHandler.returnResult(SelectCUFolderNavKey.RESULT, nodeId)
            }
            nodePickerDestination<SelectCUFolderNavKey>(
                explorerMode = ExplorerMode.SelectCUFolder,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFolderPicked = onSelectCUFolder,
            )
            val onImportFolder: (NodeId) -> Unit = { nodeId ->
                navigationHandler.returnResult(ImportNavKey.RESULT, nodeId)
            }
            nodePickerDestination<ImportNavKey>(
                explorerMode = ExplorerMode.Import,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFolderPicked = onImportFolder,
            )
            val onImportAlbumFolder: (NodeId) -> Unit = { nodeId ->
                navigationHandler.returnResult(ImportAlbumNavKey.RESULT, nodeId)
            }
            nodePickerDestination<ImportAlbumNavKey>(
                explorerMode = ExplorerMode.AlbumImport,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFolderPicked = onImportAlbumFolder,
            )
            val onCopyResult: (CopyResult) -> Unit = { result ->
                navigationHandler.returnResult(CopyNavKey.RESULT, result)
            }
            copyDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onCopyResult = onCopyResult,
            )
            val onMoveResult: (MoveResult) -> Unit = { result ->
                navigationHandler.returnResult(MoveNavKey.RESULT, result)
            }
            moveDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onMoveResult = onMoveResult,
            )
            val onFilesPicked: (List<NodeId>) -> Unit = { nodeIds ->
                navigationHandler.returnResult(ShareFilesToChatNavKey.RESULT, nodeIds)
            }
            nodePickerDestination<ShareFilesToChatNavKey>(
                explorerMode = ExplorerMode.ShareFilesToChat,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFilesPicked = { key, nodeIds ->
                    onFilesPicked(nodeIds)
                    navigationHandler.remove(key)
                },
            )
            val onVideosPicked: (List<NodeId>) -> Unit = { nodeIds ->
                navigationHandler.returnResult(AddVideoToPlaylistNavKey.RESULT, nodeIds)
            }
            nodePickerDestination<AddVideoToPlaylistNavKey>(
                explorerMode = ExplorerMode.AddVideosToPlaylist,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                disabledNodeIds = { it.addedVideoIds.toSet() },
                onFilesPicked = { key, nodeIds ->
                    onVideosPicked(nodeIds)
                    navigationHandler.remove(key)
                },
            )
            val onPinnedItemsPicked: (List<NodeId>) -> Unit = { nodeIds ->
                navigationHandler.returnResult(PinToHomeNavKey.RESULT, nodeIds)
            }
            nodePickerDestination<PinToHomeNavKey>(
                explorerMode = ExplorerMode.PinToHome,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFilesPicked = { key, nodeIds ->
                    onPinnedItemsPicked(nodeIds)
                    navigationHandler.remove(key)
                },
            )
            nodePickerDestination<SelectSyncFolderNavKey>(
                explorerMode = ExplorerMode.SelectSyncFolder,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
            )
            nodePickerDestination<SelectStopBackupDestinationNavKey>(
                explorerMode = ExplorerMode.SelectStopBackupDestination,
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
            )
            nodeExplorerDestination(
                onCloseExplorerScreen = { navigationHandler.backTo(it, true) },
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
                onSelectCUFolder = onSelectCUFolder,
                onImportFolder = onImportFolder,
                onImportAlbumFolder = onImportAlbumFolder,
                onCopyResult = onCopyResult,
                onMoveResult = onMoveResult,
                onFilesPicked = onFilesPicked,
                onVideosPicked = onVideosPicked,
                monitorResult = navigationHandler::monitorResult,
                clearResult = navigationHandler::clearResult,
            )
        }

    fun EntryProviderScope<NavKey>.shareFilesToMegaDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
        monitorResult: (String) -> Flow<Any?>,
        clearResult: (String) -> Unit,
    ) {
        entry<ShareFilesToMegaNavKey> { key ->
            val viewModel =
                hiltViewModel<ShareFilesToMegaViewModel, ShareFilesToMegaViewModel.Factory> { factory ->
                    factory.create(ShareFilesToMegaViewModel.Args(shareUris = key.shareUris))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ShareFilesToMegaScreen(
                uiState = uiState,
                startNavKey = key,
                onNavigateBack = { onNavigateBack(key) },
                onStartUpload = onStartUpload,
                onNavigate = onNavigate,
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }

    fun EntryProviderScope<NavKey>.uploadScannedDocumentDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
    ) {
        entry<UploadScannedDocumentNavKey> { key ->
            val viewModel =
                hiltViewModel<UploadScannedDocumentsViewModel, UploadScannedDocumentsViewModel.Factory> { factory ->
                    factory.create(UploadScannedDocumentsViewModel.Args(uriPath = key.uriPath))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            UploadScannedDocumentScreen(
                uiState = uiState,
                startNavKey = key,
                onNavigateBack = { onNavigateBack(key) },
                onStartUpload = onStartUpload,
                onNavigate = onNavigate,
            )
        }
    }

    /**
     * Registers a [NodePickerScreen] destination for a flow that opens at the cloud-drive root and
     * picks either a destination folder ([onFolderPicked]) or a set of files ([onFilesPicked]).
     */
    private inline fun <reified K : ExplorerNavKey> EntryProviderScope<NavKey>.nodePickerDestination(
        explorerMode: ExplorerMode,
        noinline onNavigateBack: (NavKey) -> Unit,
        noinline onNavigate: (NavKey) -> Unit,
        crossinline disabledNodeIds: (K) -> Set<NodeId> = { emptySet() },
        noinline onFolderPicked: (NodeId) -> Unit = {},
        crossinline onFilesPicked: (K, List<NodeId>) -> Unit = { _, _ -> },
    ) {
        entry<K> { key ->
            val viewModel = hiltViewModel<NodePickerViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            NodePickerScreen(
                uiState = uiState,
                startNavKey = key,
                explorerMode = explorerMode,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                disabledNodeIds = disabledNodeIds(key),
                onFolderPicked = onFolderPicked,
                onFilesPicked = { onFilesPicked(key, it) },
            )
        }
    }

    fun EntryProviderScope<NavKey>.shareTextToMegaDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
        monitorResult: (String) -> Flow<Any?>,
        clearResult: (String) -> Unit,
    ) {
        entry<ShareTextToMegaNavKey> { key ->
            val viewModel = hiltViewModel<ShareTextToMegaViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var isProcessingAction by rememberSaveable { mutableStateOf(false) }

            rememberNewFileNameResult(
                monitorResult = monitorResult,
                clearResult = clearResult,
                startNavKey = key,
                createTextFile = { name, content ->
                    isProcessingAction = true
                    viewModel.createTextFile(name, content)
                },
            )

            ShareTextToMegaScreen(
                uiState = uiState,
                startNavKey = key,
                isProcessingAction = isProcessingAction,
                onStartUpload = onStartUpload,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onChatsSelected = { isProcessingAction = true },
                onFileUriConsumed = viewModel::onFileUriConsumed,
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }

    fun EntryProviderScope<NavKey>.copyDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (List<NavKey>) -> Unit,
        onCopyResult: (CopyResult) -> Unit,
    ) {
        entry<CopyNavKey> { key ->
            val viewModel = hiltViewModel<CopyViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TargetNodePickerScreen(
                uiState = uiState,
                startNavKey = key,
                explorerMode = ExplorerMode.Copy,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onSelectFolder = { target ->
                    onCopyResult(CopyResult(nodeIds = key.nodeIds, target = target))
                },
            )
        }
    }

    fun EntryProviderScope<NavKey>.moveDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (List<NavKey>) -> Unit,
        onMoveResult: (MoveResult) -> Unit,
    ) {
        entry<MoveNavKey> { key ->
            val viewModel = hiltViewModel<MoveViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TargetNodePickerScreen(
                uiState = uiState,
                startNavKey = key,
                explorerMode = ExplorerMode.Move,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onSelectFolder = { target ->
                    onMoveResult(MoveResult(nodeIds = key.nodeIds, target = target))
                },
                disabledTargetId = key.disabledTargetId,
            )
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onCloseExplorerScreen: (NavKey) -> Unit,
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
        onSelectCUFolder: (NodeId) -> Unit,
        onImportFolder: (NodeId) -> Unit,
        onImportAlbumFolder: (NodeId) -> Unit,
        onCopyResult: (CopyResult) -> Unit,
        onMoveResult: (MoveResult) -> Unit,
        onFilesPicked: (List<NodeId>) -> Unit,
        onVideosPicked: (List<NodeId>) -> Unit,
        monitorResult: (String) -> Flow<Any?>,
        clearResult: (String) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            val onFolderSelected: (NodeId) -> Unit = when (val nav = key.startNavKey) {
                is CopyNavKey -> { target ->
                    onCopyResult(CopyResult(nodeIds = nav.nodeIds, target = target))
                }

                is MoveNavKey -> { target ->
                    onMoveResult(MoveResult(nodeIds = nav.nodeIds, target = target))
                }

                is ImportNavKey -> onImportFolder

                is ImportAlbumNavKey -> onImportAlbumFolder

                else -> onSelectCUFolder
            }
            // Block re-selecting the source nodes' current parent as the move target.
            val disabledTargetId = (key.startNavKey as? MoveNavKey)?.disabledTargetId
            // Route picked files to the right result key based on the originating flow.
            val onFilesPickedForKey: (List<NodeId>) -> Unit =
                if (key.startNavKey is AddVideoToPlaylistNavKey) onVideosPicked else onFilesPicked
            var isProcessingAction by rememberSaveable { mutableStateOf(false) }
            val (fileUriEvent, onFileUriConsumed) =
                (key.startNavKey as? ShareTextToMegaNavKey)?.let { startNavKey ->
                    val viewModel = hiltViewModel<ShareTextToMegaViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val fileUri: StateEventWithContent<UriPath> =
                        (uiState as? ShareTextToMegaUiState.Data)?.fileUri ?: consumed()

                    rememberNewFileNameResult(
                        monitorResult = monitorResult,
                        clearResult = clearResult,
                        startNavKey = startNavKey,
                        createTextFile = { name, content ->
                            isProcessingAction = true
                            viewModel.createTextFile(name, content)
                        },
                    )

                    fileUri to viewModel::onFileUriConsumed
                } ?: (consumed() to {})

            NodesExplorerScreen(
                explorerMode = key.explorerMode,
                startNavKey = key.startNavKey,
                nodeExplorerId = key.nodeId,
                nodeSourceType = key.nodeSourceType,
                isProcessingAction = isProcessingAction,
                shareUris = key.shareUris,
                fileUriEvent = fileUriEvent,
                onCloseExplorerScreen = { onCloseExplorerScreen(key.startNavKey) },
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onStartUpload = onStartUpload,
                onFileUriConsumed = onFileUriConsumed,
                onSelectFolder = onFolderSelected,
                onFilesPicked = onFilesPickedForKey,
                disabledTargetId = disabledTargetId,
                disabledNodeIds = key.disabledNodeIds.toSet(),
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }
}
