package mega.privacy.android.feature.texteditor.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.triggered
import kotlinx.coroutines.yield
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaFloatingToolbar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Compose screen for viewing and editing text files.
 * Top bar: Back + Line numbers + More (opens Node Options Bottom Sheet). Download, Get link, Send to chat, Share are in the bottom bar.
 */
@Composable
fun TextEditorScreen(
    viewModel: TextEditorComposeViewModel,
    onBack: () -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textFieldState = rememberTextFieldState()
    val scrollState = rememberScrollState()
    val readOnly = uiState.mode == TextEditorMode.View
    val density = LocalDensity.current
    val lineHeightPx = with(density) { EditorLineHeight.toPx() }
    var pendingBackAfterSave by remember { mutableStateOf(false) }

    // When entering Edit mode, place cursor at end of first visible line so the user can edit where they're looking.
    LaunchedEffect(readOnly) {
        if (readOnly) return@LaunchedEffect
        val text = textFieldState.text.toString()
        if (text.isEmpty()) return@LaunchedEffect
        val lineStarts = buildList {
            add(0)
            for (i in text.indices) if (text[i] == '\n') add(i + 1)
            add(text.length)
        }
        val lineCount = lineStarts.size - 1
        if (lineCount <= 0) return@LaunchedEffect
        val firstVisibleLineIndex = (scrollState.value / lineHeightPx).toInt().coerceIn(0, lineCount - 1)
        val endOfLineCursor = ((lineStarts.getOrNull(firstVisibleLineIndex + 1) ?: text.length) - 1).coerceIn(0, text.length)
        textFieldState.edit { selection = TextRange(endOfLineCursor) }
    }

    // Sync ViewModel content → TextFieldState.
    // Append-only: load-more in View mode or initial chunks (content grows as superset).
    // Full replace: read-only mode or discard-restore (entire buffer replaced).
    // Ignore: while actively editing, non-append external updates are skipped.
    LaunchedEffect(uiState.content, readOnly) {
        val current = textFieldState.text.toString()
        if (current == uiState.content) return@LaunchedEffect

        val shouldAppendOnly = uiState.content.startsWith(current)
        when {
            shouldAppendOnly -> {
                val suffix = uiState.content.substring(current.length)
                if (suffix.isNotEmpty()) {
                    textFieldState.edit { append(suffix) }
                }
            }

            readOnly -> {
                val savedScroll = scrollState.value
                textFieldState.edit { replace(0, length, uiState.content) }
                yield()
                scrollState.scrollTo(savedScroll)
            }

            else -> {}
        }
    }

    BackHandler {
        when {
            uiState.showDiscardDialog -> viewModel.dismissDiscardDialog()
            uiState.mode == TextEditorMode.Edit && viewModel.isContentDirty(textFieldState.text.toString()) ->
                viewModel.requestShowDiscardDialog()
            uiState.mode == TextEditorMode.Edit -> viewModel.setViewMode()
            uiState.mode == TextEditorMode.Create -> {
                pendingBackAfterSave = true
                viewModel.saveFile(
                    currentText = textFieldState.text.toString(),
                    fromHome = false,
                )
            }
            else -> onBack()
        }
    }

    if (uiState.showDiscardDialog) {
        TextEditorDiscardDialog(
            onDiscard = viewModel::confirmDiscard,
            onCancel = viewModel::dismissDiscardDialog,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val changesSavedMessage = stringResource(sharedR.string.general_changes_saved)
    LaunchedEffect(uiState.saveSuccessEvent) {
        if (uiState.saveSuccessEvent == triggered) {
            viewModel.consumeSaveSuccessEvent()
            if (pendingBackAfterSave) {
                pendingBackAfterSave = false
                onBack()
            } else {
                snackbarHostState.showSnackbar(changesSavedMessage)
            }
        }
    }

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            MegaSnackbar(snackBarHostState = snackbarHostState)
        },
        topBar = {
            when (uiState.mode) {
                TextEditorMode.Edit -> TextEditorEditModeTopAppBar(
                    title = uiState.fileName,
                    onClose = {
                        if (viewModel.isContentDirty(textFieldState.text.toString()))
                            viewModel.requestShowDiscardDialog()
                        else viewModel.setViewMode()
                    },
                    onSave = {
                        viewModel.saveFile(
                            currentText = textFieldState.text.toString(),
                            fromHome = false,
                        )
                    },
                    onMenuAction = viewModel::onMenuAction,
                )

                else -> TextEditorViewModeTopAppBar(
                    title = uiState.fileName,
                    onBack = onBack,
                    onMenuAction = viewModel::onMenuAction,
                    onOpenNodeOptions = onOpenNodeOptions,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TextEditorLoadingContent()
                    }
                }

                uiState.errorEvent == triggered -> {
                    pendingBackAfterSave = false
                    TextEditorErrorContent(
                        message = uiState.loadErrorMessage?.takeIf { it.isNotBlank() }
                            ?: stringResource(sharedR.string.general_request_failed_message),
                        onDismiss = {
                            viewModel.consumeErrorEvent()
                            onBack()
                        },
                    )
                }

                else -> {
                    val onLoadMore = remember(viewModel) { viewModel::onLoadMoreLines }
                    val onAppendSuffixConsumed = remember(viewModel) { viewModel::consumeAppendSuffix }
                    TextEditorContent(
                        textFieldState = textFieldState,
                        scrollState = scrollState,
                        showLineNumbers = uiState.showLineNumbers,
                        readOnly = readOnly,
                        appendSuffix = uiState.appendSuffix,
                        onAppendSuffixConsumed = onAppendSuffixConsumed,
                        hasMoreLines = uiState.hasMoreLines,
                        onNearEndOfScroll = onLoadMore,
                    )
                }
            }
            if (uiState.isRestoringContent) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LargeInfiniteSpinnerIndicator()
                }
            }
            TextEditorBottomBar(
                visible = uiState.mode != TextEditorMode.Edit && uiState.bottomBarActions.isNotEmpty() && !uiState.isLoading,
                actions = uiState.bottomBarActions,
                onActionPressed = { action ->
                    (action as? TextEditorBottomBarAction)?.let { viewModel.onBottomBarAction(it) }
                },
            )
        }
    }
}

@Composable
private fun TextEditorErrorContent(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MegaText(text = message, modifier = Modifier.padding(16.dp))
            Button(onClick = onDismiss) {
                Text(text = stringResource(sharedR.string.general_ok))
            }
        }
    }
}

/**
 * Loading view matching legacy text editor: text file icon (96dp) and horizontal indeterminate progress bar below.
 * Legacy: loading_layout has layout_constraintTop_toTopOf="parent" and layout_marginTop="153dp", so content is
 * top-anchored 153dp from the top of the screen (not vertically centered).
 */
@Composable
private fun TextEditorLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 153.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(96.dp),
            painter = painterResource(IconPackR.drawable.ic_text_medium_solid),
            contentDescription = stringResource(sharedR.string.transfers_fake_preview_text),
        )
        Spacer(modifier = Modifier.height(30.dp))
        InfiniteProgressBarIndicator(
            modifier = Modifier
                .widthIn(min = 100.dp)
                .padding(horizontal = 44.dp),
        )
    }
}

@Composable
private fun TextEditorBottomBar(
    visible: Boolean,
    actions: List<MenuActionWithIcon>,
    onActionPressed: (MenuActionWithIcon) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { it },
        ),
        exit = ExitTransition.None,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
        ) {
            MegaFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                actions = actions,
                actionsEnabled = true,
                onActionPressed = onActionPressed,
            )
        }
    }
}

@Composable
private fun TextEditorDiscardDialog(
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicDialog(
        title = stringResource(sharedR.string.general_dialog_title_discard_changes),
        description = stringResource(sharedR.string.general_dialog_discard_changes_message),
        positiveButtonText = stringResource(sharedR.string.general_dialog_discard_button),
        onPositiveButtonClicked = onDiscard,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onCancel,
        onDismiss = onCancel,
    )
}

@Composable
private fun TextEditorEditModeTopAppBar(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
) {
    MegaTopAppBar(
        title = title,
        navigationType = AppBarNavigationType.Close(onClose),
        actions = listOf(
            TextEditorTopBarAction.LineNumbers,
            TextEditorTopBarAction.Save,
        ),
        onActionPressed = {
            when (it) {
                is TextEditorTopBarAction.Save -> onSave()
                is TextEditorTopBarAction -> onMenuAction(it)
                else -> Unit
            }
        },
    )
}

@Composable
private fun TextEditorViewModeTopAppBar(
    title: String,
    onBack: () -> Unit,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    MegaTopAppBar(
        title = title,
        navigationType = AppBarNavigationType.Back(onBack),
        actions = listOf(
            TextEditorTopBarAction.LineNumbers,
            TextEditorTopBarAction.More,
        ),
        onActionPressed = {
            when (it) {
                is TextEditorTopBarAction.More -> onOpenNodeOptions()
                is TextEditorTopBarAction -> onMenuAction(it)
                else -> Unit
            }
        },
    )
}
