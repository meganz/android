package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files.ShareFilesToMegaViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaUiState
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey

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
            nodeExplorerDestination(
                onCloseExplorerScreen = { navigationHandler.backTo(it, true) },
                onNavigateBack = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
                onStartUpload = transferHandler::setTransferEvent,
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
                onFileUriConsumed = viewModel::onFileUriConsumed,
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }

    fun EntryProviderScope<NavKey>.nodeExplorerDestination(
        onCloseExplorerScreen: (NavKey) -> Unit,
        onNavigateBack: (NavKey) -> Unit,
        onNavigate: (NavKey) -> Unit,
        onStartUpload: (TransferTriggerEvent) -> Unit,
        monitorResult: (String) -> Flow<Any?>,
        clearResult: (String) -> Unit,
    ) {
        entry<NodesExplorerNavKey> { key ->
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
                monitorResult = monitorResult,
                clearResult = clearResult,
            )
        }
    }
}
