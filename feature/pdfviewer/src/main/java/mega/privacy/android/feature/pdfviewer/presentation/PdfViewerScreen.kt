package mega.privacy.android.feature.pdfviewer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfSearchResultsBar
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerContent
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerSearchTopBar
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerTopBar
import mega.privacy.android.feature.pdfviewer.presentation.components.getPdfUri
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError

/**
 * Stateless PDF Viewer screen.
 *
 * @param uiState The current UI state
 * @param onBack Callback for back navigation
 * @param onMoreClicked Callback when more options is clicked
 * @param onPageChanged Callback when page changes with (currentPage, totalPages)
 * @param onLoadComplete Callback when PDF load completes with total pages
 * @param onError Callback when an error occurs
 * @param onSubmitPassword Callback to submit password for encrypted PDF
 * @param onDismissPasswordDialog Callback to dismiss the password dialog
 * @param onRetry Callback to retry loading
 * @param onUploadToCloudDrive Callback to upload file to cloud drive
 * @param onActivateSearch Callback to activate search mode
 * @param onDeactivateSearch Callback to deactivate search mode
 * @param onSearchQueryChanged Callback when search query changes
 * @param onNavigateToNextMatch Callback to navigate to next search match
 * @param onNavigateToPreviousMatch Callback to navigate to previous search match
 * @param modifier Modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfViewerScreen(
    uiState: PdfViewerState,
    onBack: () -> Unit,
    onMoreClicked: () -> Unit,
    onPageChanged: (Int, Int) -> Unit,
    onLoadComplete: (Int) -> Unit,
    onError: (PdfViewerError) -> Unit,
    onSubmitPassword: (String) -> Unit,
    onDismissPasswordDialog: () -> Unit,
    onRetry: () -> Unit,
    onUploadToCloudDrive: () -> Unit,
    onActivateSearch: () -> Unit,
    onDeactivateSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNavigateToNextMatch: () -> Unit,
    onNavigateToPreviousMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = uiState.searchState
    val showPasswordOverlay = uiState.showPasswordDialog || uiState.isPasswordError

    BackHandler(searchState.isSearchActive) {
        onDeactivateSearch()
    }

    val pdfUri = remember(uiState.source) { getPdfUri(uiState.source) }
    val bytes = uiState.pdfBytes?.bytes

    Box(modifier = modifier.fillMaxSize()) {
        MegaScaffoldWithTopAppBarScrollBehavior(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (searchState.isSearchActive) {
                    PdfViewerSearchTopBar(
                        query = searchState.query,
                        onQueryChanged = onSearchQueryChanged,
                        onClose = onDeactivateSearch,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PdfViewerTopBar(
                        title = uiState.title,
                        onBack = onBack,
                        onSearch = onActivateSearch,
                        onOpenNodeOptions = onMoreClicked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Only show loading spinner when waiting for remote bytes to download
                    // For local content, render PdfViewerContent immediately and let PDFView handle its own loading
                    val showLoading = uiState.source?.isRemote == true && uiState.pdfBytes == null

                    when {
                        showLoading -> {
                            InfiniteProgressBarIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .widthIn(min = 100.dp)
                                    .padding(horizontal = 44.dp),
                            )
                        }

                        uiState.error != null && !uiState.isPasswordError -> {
                            // TODO: Handler Error
                        }

                        uiState.source != null -> {
                            PdfViewerContent(
                                pdfUri = pdfUri,
                                pdfBytes = bytes,
                                currentPage = uiState.currentPage,
                                password = uiState.currentPassword,
                                highlightPageIndex = searchState.currentMatchPageIndex,
                                highlightPdfRects = searchState.currentMatchPdfRects,
                                onPageChanged = onPageChanged,
                                onLoadComplete = onLoadComplete,
                                onError = onError,
                                onTap = { /* toolbar is always visible */ },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    // Floating page indicator (top-end) - transient overlay
                    if (!searchState.isSearchActive) {
                        // TODO: show PdfPageIndicatorOverlay when page is changing, then hide after a delay
                    }

                    // Floating search results bar (bottom-center)
                    if (searchState.isSearchActive && searchState.hasResults) {
                        PdfSearchResultsBar(
                            label = uiState.searchState.label,
                            onPrev = onNavigateToPreviousMatch,
                            onNext = onNavigateToNextMatch,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    // Bottom bar for external files
                    if (!searchState.isSearchActive) {
                        // TODO: Show PDFViewer bottom bar for external files with "Upload to Cloud Drive" action
                    }
                }
            }
        )

        if (showPasswordOverlay) {
            // TODO: Show PdfViewerPasswordDialog (onSubmitPassword / onDismissPasswordDialog). If dialog uses onDismissRequest for back, remove password branch from BackHandler.
        }
    }
}
