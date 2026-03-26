package mega.privacy.android.feature.pdfviewer.presentation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.PdfViewerNavKey

/**
 * Extension function to register the PDF viewer screen in the navigation graph.
 *
 * @param navigationHandler Handler for navigation events and result monitoring
 * @param onBack Callback for back navigation
 * @param onOpenNodeOptions Callback to open node options bottom sheet
 * @param onTransfer Callback to handle transfer events
 */
internal fun EntryProviderScope<NavKey>.pdfViewerScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onOpenNodeOptions: (Long, NodeSourceType) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PdfViewerNavKey> { navKey ->
        val viewModel = hiltViewModel<PdfViewerViewModel, PdfViewerViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(
                    PdfViewerViewModel.Args(
                        nodeHandle = navKey.nodeHandle,
                        contentUri = navKey.contentUri,
                        isLocalContent = navKey.isLocalContent,
                        nodeSourceType = navKey.nodeSourceType,
                        mimeType = navKey.mimeType,
                        title = navKey.title,
                        chatId = navKey.chatId,
                        messageId = navKey.messageId,
                        shouldStopHttpServer = navKey.shouldStopHttpServer,
                    )
                )
            }
        )

        val uiState by viewModel.state.collectAsStateWithLifecycle()

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(navKey.nodeSourceType) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        PdfViewerScreen(
            uiState = uiState,
            onBack = onBack,
            onMoreClicked = { onOpenNodeOptions(uiState.nodeHandle, uiState.nodeSourceType) },
            onPageChanged = viewModel::onPageChanged,
            onLoadComplete = viewModel::onLoadComplete,
            onError = viewModel::onLoadError,
            onSubmitPassword = viewModel::submitPassword,
            onDismissPasswordDialog = {
                viewModel.dismissPasswordDialog()
                onBack()
            },
            onRetry = viewModel::retryLoad,
            onUploadToCloudDrive = onBack,
            onActivateSearch = viewModel::activateSearch,
            onDeactivateSearch = viewModel::deactivateSearch,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onNavigateToNextMatch = viewModel::navigateToNextMatch,
            onNavigateToPreviousMatch = viewModel::navigateToPreviousMatch,
        )
    }
}
