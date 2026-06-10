package mega.privacy.android.feature.pdfviewer.presentation

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.FileExplorerNavKey
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.setPendingDeepLink
import mega.privacy.android.shared.nodes.sheet.PublicLinkAuthAlertBottomSheet
import mega.privacy.android.shared.nodes.sheet.PublicLinkType

/**
 * Extension function to register the PDF viewer screen in the navigation graph.
 *
 * @param navigationHandler Handler for navigation events and result monitoring
 * @param onBack Callback for back navigation
 * @param onOpenNodeOptions Callback to open node options bottom sheet
 * @param onTransfer Callback to handle transfer events
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.pdfViewerScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onOpenNodeOptions: (Long, NodeSourceType) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PdfViewerNavKey> { navKey ->
        val activity = LocalActivity.current
        var showLoginRequiredSheet by rememberSaveable { mutableStateOf(false) }
        val pdfViewerOnBack: () -> Unit = remember(navKey.isExternalFile, onBack, activity) {
            {
                if (navKey.isExternalFile && activity != null) {
                    activity.finish()
                } else {
                    onBack()
                }
            }
        }

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
                        isExternalFile = navKey.isExternalFile,
                    )
                )
            }
        )

        val uiState by viewModel.state.collectAsStateWithLifecycle()

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(navKey.nodeSourceType) }
            )

        val singleNodeActionHandler = rememberSingleNodeActionHandler(
            viewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
        )

        val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

        // Push the current PDF node into the action ViewModel so it can compute the
        // floating-toolbar actions through the same selection-mode pipeline that Cloud
        // Drive uses. Re-run whenever the node or its source type changes.
        LaunchedEffect(uiState.currentNode, uiState.nodeSourceType) {
            val node = uiState.currentNode ?: return@LaunchedEffect
            nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
                selectedNodes = setOf(node),
                nodeSourceType = uiState.nodeSourceType,
            )
        }

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            nodeActionHandler = singleNodeActionHandler,
            onTransfer = onTransfer,
        )

        EventEffect(
            event = uiState.dismissEvent,
            onConsumed = viewModel::resetDismissEvent,
            action = pdfViewerOnBack,
        )

        PdfViewerScreen(
            uiState = uiState,
            onBack = pdfViewerOnBack,
            onMoreClicked = { onOpenNodeOptions(uiState.nodeHandle, uiState.nodeSourceType) },
            onPageChanged = viewModel::onPageChanged,
            onLoadComplete = viewModel::onLoadComplete,
            onError = viewModel::onLoadError,
            onSubmitPassword = viewModel::submitPassword,
            onDismissPasswordDialog = {
                viewModel.clearError()
                pdfViewerOnBack()
            },
            onDismissErrorDialog = {
                viewModel.clearError()
                pdfViewerOnBack()
            },
            onPasswordInputChanged = viewModel::onPasswordDialogInputChanged,
            onRetry = viewModel::retryLoad,
            onActivateSearch = viewModel::activateSearch,
            onDeactivateSearch = viewModel::deactivateSearch,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onNavigateToNextMatch = viewModel::navigateToNextMatch,
            onNavigateToPreviousMatch = viewModel::navigateToPreviousMatch,
            onUploadToCloudDrive = {
                if (!nodeActionState.isLoggedIn) {
                    // Not logged in: prompt to log in / sign up. The content URI is stashed via
                    // setPendingDeepLink so the host re-emits the PDF deep link after
                    // authentication and the user can tap Save again.
                    showLoginRequiredSheet = true
                } else if (uiState.isFileExplorerEnabled) {
                    navigationHandler.navigate(
                        ShareFilesToMegaNavKey(listOf(UriPath(navKey.contentUri)))
                    )
                } else {
                    navigationHandler.navigate(
                        FileExplorerNavKey(
                            action = Intent.ACTION_SEND,
                            shareUri = navKey.contentUri,
                            mimeType = navKey.mimeType,
                        )
                    )
                }
            },
            // The top bar's overflow already opens the full NodeOptionsBottomSheet, so
            // dropping the floating toolbar's More entry keeps the bar to the same 4 most
            // common actions matching the design.
            bottomBarActions = nodeActionState.visibleActions
                .filterNot { it is CommonMenuAction.More },
            singleNodeActionHandler = singleNodeActionHandler,
        )

        if (showLoginRequiredSheet) {
            PublicLinkAuthAlertBottomSheet(
                type = PublicLinkType.ExternalFile,
                onSignupClicked = {
                    showLoginRequiredSheet = false
                    activity.setPendingDeepLink(navKey.contentUri, navKey.mimeType)
                    navigationHandler.navigate(CreateAccountNavKey())
                },
                onLoginClicked = {
                    showLoginRequiredSheet = false
                    activity.setPendingDeepLink(navKey.contentUri, navKey.mimeType)
                    navigationHandler.navigate(LoginNavKey())
                },
                onDismissSheet = { showLoginRequiredSheet = false },
            )
        }
    }
}
