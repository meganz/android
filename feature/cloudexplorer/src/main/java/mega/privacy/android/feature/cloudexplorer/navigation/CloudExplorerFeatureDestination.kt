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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.copy.CopyScreen
import mega.privacy.android.feature.cloudexplorer.presentation.copy.CopyViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.selectcufolder.SelectCUFolderScreen
import mega.privacy.android.feature.cloudexplorer.presentation.selectcufolder.SelectCUFolderViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.sharefilestochat.ShareFilesToChatScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharefilestochat.ShareFilesToChatViewModel
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
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.SelectCUFolderNavKey
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
            selectCUFolderDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onSelectFolder = onSelectCUFolder,
            )
            val onCopyFolder: (NodeId) -> Unit = { nodeId ->
                navigationHandler.returnResult(CopyNavKey.RESULT, nodeId)
            }
            copyDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onSelectFolder = onCopyFolder,
            )
            val onFilesPicked: (List<NodeId>) -> Unit = { nodeIds ->
                navigationHandler.returnResult(ShareFilesToChatNavKey.RESULT, nodeIds)
            }
            shareFilesToChatDestination(
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onFilesPicked = onFilesPicked,
            )
            nodeExplorerDestination(
                onCloseExplorerScreen = { navigationHandler.backTo(it, true) },
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
                onSelectCUFolder = onSelectCUFolder,
                onCopyFolder = onCopyFolder,
                onFilesPicked = onFilesPicked,
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

    fun EntryProviderScope<NavKey>.selectCUFolderDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onSelectFolder: (NodeId) -> Unit,
    ) {
        entry<SelectCUFolderNavKey> { key ->
            val viewModel = hiltViewModel<SelectCUFolderViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SelectCUFolderScreen(
                uiState = uiState,
                startNavKey = key,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onSelectFolder = onSelectFolder,
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

    fun EntryProviderScope<NavKey>.shareFilesToChatDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onFilesPicked: (List<NodeId>) -> Unit,
    ) {
        entry<ShareFilesToChatNavKey> { key ->
            val viewModel = hiltViewModel<ShareFilesToChatViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ShareFilesToChatScreen(
                uiState = uiState,
                startNavKey = key,
                onFilesPicked = onFilesPicked,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
            )
        }
    }

    fun EntryProviderScope<NavKey>.copyDestination(
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (List<NavKey>) -> Unit,
        onSelectFolder: (NodeId) -> Unit,
    ) {
        entry<CopyNavKey> { key ->
            val viewModel = hiltViewModel<CopyViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CopyScreen(
                uiState = uiState,
                startNavKey = key,
                onNavigateBack = { onNavigateBack(key) },
                onNavigate = onNavigate,
                onSelectFolder = onSelectFolder,
            )
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onCloseExplorerScreen: (NavKey) -> Unit,
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
        onSelectCUFolder: (NodeId) -> Unit,
        onCopyFolder: (NodeId) -> Unit,
        onFilesPicked: (List<NodeId>) -> Unit,
        monitorResult: (String) -> Flow<Any?>,
        clearResult: (String) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
            val onFolderSelected = when (key.startNavKey) {
                CopyNavKey -> onCopyFolder
                else -> onSelectCUFolder
            }
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
                onNavigate = { onNavigate(it) },
                onStartUpload = onStartUpload,
                onFileUriConsumed = onFileUriConsumed,
                onSelectFolder = onFolderSelected,
                onFilesPicked = onFilesPicked,
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }
}
