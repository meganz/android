package mega.privacy.android.feature.pdfviewer.presentation

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.buildDownloadAwareActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.pdfviewer.presentation.components.startPdfFileShareIntent
import mega.privacy.android.feature.pdfviewer.presentation.components.startPdfPublicLinkShareIntent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.FileExplorerNavKey
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.setPendingDeepLink
import mega.privacy.android.shared.nodes.sheet.PublicLinkAuthAlertBottomSheet
import mega.privacy.android.shared.nodes.sheet.PublicLinkType
import mega.privacy.mobile.analytics.event.PdfViewerScreenEvent

/**
 * Extension function to register the PDF viewer screen in the navigation graph.
 *
 * @param navigationHandler Handler for navigation events and result monitoring
 * @param onBack Callback for back navigation
 * @param onOpenNodeOptions Callback to open node options bottom sheet with
 * (nodeHandle, nodeSourceType, publicLinkUrl, chatId, messageId)
 * @param onTransfer Callback to handle transfer events
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.pdfViewerScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onOpenNodeOptions: (Long, NodeSourceType, String?, Long?, Long?) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PdfViewerNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(PdfViewerScreenEvent)
        }
    ) { navKey ->
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
                        publicLinkUrl = navKey.publicLinkUrl,
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

        val downloadAwareActionHandler =
            remember(singleNodeActionHandler, nodeOptionsActionViewModel) {
                buildDownloadAwareActionHandler(
                    delegate = singleNodeActionHandler,
                    onDownload = { node ->
                        nodeOptionsActionViewModel.updateSelectedNodes(listOf(node))
                        nodeOptionsActionViewModel.downloadNode(withStartMessage = true)
                    },
                )
            }

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
            nodeActionHandler = downloadAwareActionHandler,
            onTransfer = onTransfer,
        )

        // "Save to MEGA" on a file-link PDF (floating toolbar / More sheet) routes through
        // SaveToMegaActionClickHandler, which raises loginRequiredEvent when logged out.
        // Consume it here to prompt sign in / sign up, mirroring the file-link screen.
        EventEffect(
            event = nodeActionState.loginRequiredEvent,
            onConsumed = nodeOptionsActionViewModel::resetLoginRequiredEvent,
        ) {
            showLoginRequiredSheet = true
        }

        EventEffect(
            event = uiState.dismissEvent,
            onConsumed = viewModel::resetDismissEvent,
            action = pdfViewerOnBack,
        )

        PdfViewerScreen(
            uiState = uiState,
            onBack = pdfViewerOnBack,
            onMoreClicked = {
                // Forward the file-link URL so the node-options sheet resolves the public node
                // (a file-link node is not in the account); null for non-file-link sources.
                // Forward the chat ids so the sheet can resolve chat files received from
                // others (their node is not in the account) via GetChatFileUseCase.
                onOpenNodeOptions(
                    uiState.nodeHandle,
                    uiState.nodeSourceType,
                    navKey.publicLinkUrl,
                    navKey.chatId,
                    navKey.messageId,
                )
            },
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
            singleNodeActionHandler = downloadAwareActionHandler,
            // Reuse one Share action for the two surfaces that have something to share:
            //  • file link  → share the original public-link URL (mirrors the file-link screen);
            //  • external   → share the file itself via its content URI (mirrors legacy share).
            // Null for in-account sources, where Share lives in the node-options sheet instead.
            onShare = when {
                navKey.publicLinkUrl != null -> {
                    { activity?.startPdfPublicLinkShareIntent(navKey.publicLinkUrl, uiState.title) }
                }

                navKey.isExternalFile -> {
                    {
                        activity?.startPdfFileShareIntent(
                            contentUri = navKey.contentUri,
                            mimeType = navKey.mimeType,
                            title = uiState.title,
                        )
                    }
                }

                else -> null
            },
        )

        if (showLoginRequiredSheet) {
            // A file-link PDF resumes to its public link after auth; an external file
            // resumes to its content URI so the user can tap Save again.
            val isFileLink = navKey.publicLinkUrl != null
            val stashPendingDeepLink: () -> Unit = {
                if (isFileLink) {
                    activity.setPendingDeepLink(navKey.publicLinkUrl)
                } else {
                    activity.setPendingDeepLink(navKey.contentUri, navKey.mimeType)
                }
            }
            PdfViewerLoginRequiredSheet(
                isFileLink = isFileLink,
                onSignupClicked = {
                    showLoginRequiredSheet = false
                    stashPendingDeepLink()
                    navigationHandler.navigate(CreateAccountNavKey())
                },
                onLoginClicked = {
                    showLoginRequiredSheet = false
                    stashPendingDeepLink()
                    navigationHandler.navigate(LoginNavKey())
                },
                onDismissSheet = { showLoginRequiredSheet = false },
            )
        }
    }
}

/**
 * Login / sign-up prompt shown when a logged-out user taps "Save to MEGA" or the
 * external-file upload action. A file-link PDF uses [PublicLinkType.File]; an external
 * file uses [PublicLinkType.ExternalFile].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfViewerLoginRequiredSheet(
    isFileLink: Boolean,
    onSignupClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onDismissSheet: () -> Unit,
) {
    PublicLinkAuthAlertBottomSheet(
        type = if (isFileLink) PublicLinkType.File else PublicLinkType.ExternalFile,
        onSignupClicked = onSignupClicked,
        onLoginClicked = onLoginClicked,
        onDismissSheet = onDismissSheet,
    )
}
