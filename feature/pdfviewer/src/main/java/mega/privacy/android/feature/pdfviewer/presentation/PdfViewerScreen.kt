package mega.privacy.android.feature.pdfviewer.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.feature.pdfviewer.presentation.components.ExternalFileBottomBar
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfPageIndicator
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfSearchResultsBar
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerContent
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerErrorDialog
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerPasswordDialog
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerSearchTopBar
import mega.privacy.android.feature.pdfviewer.presentation.components.PdfViewerTopBar
import mega.privacy.android.feature.pdfviewer.presentation.components.getPdfUri
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.shared.resources.R as sharedR

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
 * @param onDismissErrorDialog Callback when the non-password error dialog is dismissed
 * @param onPasswordInputChanged Callback when the user edits the password input field
 * @param onRetry Callback to retry loading
 * @param onUploadToCloudDrive Callback to upload file to cloud drive
 * @param onActivateSearch Callback to activate search mode
 * @param onDeactivateSearch Callback to deactivate search mode
 * @param onSearchQueryChanged Callback when search query changes
 * @param onNavigateToNextMatch Callback to navigate to next search match
 * @param onNavigateToPreviousMatch Callback to navigate to previous search match
 * @param bottomBarActions Actions to render in the floating toolbar. Sourced from
 *  [NodeOptionsActionViewModel] in the destination — pass an empty list to hide the toolbar.
 * @param singleNodeActionHandler Dispatcher for floating-toolbar actions on the current node
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
    onDismissErrorDialog: () -> Unit,
    onPasswordInputChanged: () -> Unit,
    onRetry: () -> Unit,
    onUploadToCloudDrive: () -> Unit,
    onActivateSearch: () -> Unit,
    onDeactivateSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNavigateToNextMatch: () -> Unit,
    onNavigateToPreviousMatch: () -> Unit,
    bottomBarActions: List<MenuActionWithIcon>,
    singleNodeActionHandler: SingleNodeActionHandler,
    modifier: Modifier = Modifier,
) {
    val searchState = uiState.searchState

    var showPasswordOverlay by rememberSaveable { mutableStateOf(false) }
    // Enable isAutoShowKeyboard after rotate screen
    var passwordDialogAutoKeyboard by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isPasswordError, uiState.error) {
        if (!uiState.isPasswordError) {
            showPasswordOverlay = false
        } else if (uiState.error is PdfViewerError.InvalidPassword) {
            //  Second time showing the dialog (InvalidPassword)
            //  Delay re-showing the dialog after a wrong password attempt to avoid keyboard flicker.
            delay(PASSWORD_DIALOG_DELAY_MS)
            passwordDialogAutoKeyboard = false
            showPasswordOverlay = true
        } else if (!showPasswordOverlay) {
            // First time showing the dialog (PasswordProtected): enable auto keyboard.
            passwordDialogAutoKeyboard = true
            showPasswordOverlay = true
        }
    }

    BackHandler(searchState.isSearchActive) {
        onDeactivateSearch()
    }

    // External opens: route hardware back through onBack so the activity finishes instead of revealing the underlying stack.
    BackHandler(enabled = uiState.isExternalFile && !searchState.isSearchActive) {
        onBack()
    }

    val pdfUri = remember(uiState.source) { getPdfUri(uiState.source) }
    val bytes = uiState.pdfBytes?.bytes
    val currentPage = uiState.currentPage

    // Null when not actively dragging; 0f..1f while the user drags the page indicator's thumb.
    var scrubProgress by remember { mutableStateOf<Float?>(null) }

    // True from the moment the thumb is pressed until the press or drag ends.
    // Lets the PDF view stop an in-flight fling on press without committing a new scroll position.
    var isScrubPressed by remember { mutableStateOf(false) }

    var indicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage, uiState.totalPages, scrubProgress, isScrubPressed) {
        if (uiState.totalPages > 1) {
            indicatorVisible = true
            if (scrubProgress == null && !isScrubPressed) {
                delay(PAGE_INDICATOR_AUTO_HIDE_MS)
                indicatorVisible = false
            }
        } else {
            indicatorVisible = false
        }
    }

    // Show loading until the initial page is resolved and remote content downloaded.
    val showLoading = currentPage == null ||
            (uiState.source?.isRemote == true && uiState.pdfBytes == null)

    val showFloatingToolbar = !showLoading
            && !uiState.isExternalFile
            && !searchState.isSearchActive
            && !uiState.isPasswordError
            && uiState.error == null
            && bottomBarActions.isNotEmpty()
            && uiState.currentNode != null

    // Toggled by tapping the document while reading. Starts visible.
    var chromeVisible by rememberSaveable { mutableStateOf(true) }

    // Hide system bars with the chrome; swipe restores them transiently.
    val view = LocalView.current
    val activity = LocalActivity.current
    LaunchedEffect(chromeVisible, activity, view) {
        val window = activity?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (chromeVisible) {
                show(WindowInsetsCompat.Type.systemBars())
            } else {
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    // Restore system bars on exit so the next screen isn't left immersive.
    DisposableEffect(activity, view) {
        onDispose {
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MegaScaffoldWithTopAppBarScrollBehavior(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showLoading.not()) {
                    if (searchState.isSearchActive) {
                        PdfViewerSearchTopBar(
                            query = searchState.query,
                            onQueryChanged = onSearchQueryChanged,
                            onClose = onDeactivateSearch,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AnimatedVisibility(
                            visible = chromeVisible,
                            enter = slideInVertically { -it } + fadeIn(),
                            exit = slideOutVertically { -it } + fadeOut(),
                        ) {
                            PdfViewerTopBar(
                                title = uiState.title,
                                onBack = onBack,
                                onSearch = onActivateSearch,
                                onOpenNodeOptions = onMoreClicked,
                                showMoreAction = !uiState.isExternalFile,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (uiState.isExternalFile && !searchState.isSearchActive) {
                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        ExternalFileBottomBar(onUploadToCloudDrive = onUploadToCloudDrive)
                    }
                } else {
                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        SelectionModeBottomBar(
                            visible = showFloatingToolbar,
                            actions = bottomBarActions,
                            onActionPressed = { action ->
                                uiState.currentNode?.let { node ->
                                    singleNodeActionHandler(action, node)
                                }
                            },
                        )
                    }
                }
            },
            content = { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {

                    // PDF deliberately ignores innerPadding so the page doesn't resize/jump when chrome toggles.
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
                            PdfViewerErrorDialog(
                                error = uiState.error,
                                isOnline = uiState.isOnline,
                                onDismiss = onDismissErrorDialog,
                            )
                        }

                        // Note: when isPasswordError=true, no branch matches intentionally.
                        // The password dialog is rendered as a full-screen overlay below (outside the scaffold).

                        !uiState.isPasswordError && uiState.source != null && currentPage != null -> {
                            PdfViewerContent(
                                pdfUri = pdfUri,
                                pdfBytes = bytes,
                                currentPage = currentPage,
                                password = uiState.currentPassword,
                                highlightPageIndex = searchState.currentMatchPageIndex,
                                highlightPdfRects = searchState.currentMatchPdfRects,
                                allMatchRectsByPage = searchState.allMatchRectsByPage,
                                scrubProgress = scrubProgress,
                                isScrubPressed = isScrubPressed,
                                onPageChanged = onPageChanged,
                                onLoadComplete = onLoadComplete,
                                onError = onError,
                                // Suppressed during search so a tap doesn't pull the search bar away.
                                onTap = {
                                    if (!searchState.isSearchActive) {
                                        chromeVisible = !chromeVisible
                                    }
                                },
                                // Suppressed during search so the search bar stays put.
                                onChromeVisibilityChange = { visible ->
                                    if (!searchState.isSearchActive) {
                                        chromeVisible = visible
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    // Reserve the toolbar-occupied insets for the scrubber as a CONSTANT band:
                    //  • using the live innerPadding makes the thumb jump when the toolbars toggle
                    //    (the track height changes), and
                    //  • spanning the full height lets the toolbars cover the thumb at the extremes.
                    // So we keep the largest top/bottom inset seen — captured while the toolbars are
                    // visible (they are on open) — so the track stays constant AND in the gap
                    // between the bars. maxOf never shrinks, so toggling the toolbars is a no-op.
                    var reservedTopInset by remember { mutableStateOf(0.dp) }
                    var reservedBottomInset by remember { mutableStateOf(0.dp) }
                    reservedTopInset = maxOf(reservedTopInset, innerPadding.calculateTopPadding())
                    reservedBottomInset =
                        maxOf(reservedBottomInset, innerPadding.calculateBottomPadding())

                    // Fast-scroll page indicator — draggable thumb on the right edge that also
                    // reflects the current scroll position.
                    if (currentPage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = reservedTopInset, bottom = reservedBottomInset)
                        ) {
                            PdfPageIndicator(
                                currentPage = currentPage,
                                totalPages = uiState.totalPages,
                                // scrubProgress != null / isScrubPressed guard the race window where the
                                // auto-hide timer fires (e.g. jump to next search result) before
                                // LaunchedEffect re-runs when a press or drag begins.
                                isVisible = indicatorVisible || scrubProgress != null || isScrubPressed,
                                onScrub = { scrubProgress = it },
                                onScrubPressed = { isScrubPressed = it },
                            )
                        }
                    }

                    // Floating search results bar (bottom-center)
                    if (searchState.isSearchActive && searchState.hasResults) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            PdfSearchResultsBar(
                                label = uiState.searchState.label,
                                onPrev = onNavigateToPreviousMatch,
                                onNext = onNavigateToNextMatch,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        )

        if (showPasswordOverlay) {
            // Pre-fills with the last attempted password so the user can review and correct it.
            var localPassword by rememberSaveable { mutableStateOf(uiState.currentPassword ?: "") }
            val errorText =
                if (uiState.error is PdfViewerError.InvalidPassword) {
                    stringResource(sharedR.string.pdf_viewer_dialog_error_incorrect_password)
                } else {
                    null
                }

            PdfViewerPasswordDialog(
                password = localPassword,
                errorText = errorText,
                onPasswordChange = {
                    localPassword = it
                    onPasswordInputChanged()
                },
                onConfirm = { onSubmitPassword(localPassword) },
                onDismiss = onDismissPasswordDialog,
                isAutoShowKeyboard = passwordDialogAutoKeyboard,
            )
        }
    }
}

private const val PASSWORD_DIALOG_DELAY_MS = 500L
private const val PAGE_INDICATOR_AUTO_HIDE_MS = 2000L
