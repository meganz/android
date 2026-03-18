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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.triggered
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaFloatingToolbar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.math.abs

/**
 * Compose screen for viewing and editing text files.
 */
@Composable
fun TextEditorScreen(
    viewModel: TextEditorComposeViewModel,
    onBack: () -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val isEditable = uiState.mode == TextEditorMode.Edit || uiState.mode == TextEditorMode.Create
    var pendingBackAfterSave by remember { mutableStateOf(false) }

    val chunkTextProvider = remember(viewModel, uiState.contentVersion) {
        { idx: Int -> viewModel.getChunkText(idx) }
    }
    val chunkStateProvider: ((Int) -> TextFieldState)? =
        if (isEditable) {
            remember(viewModel, uiState.contentVersion) {
                { idx: Int -> viewModel.getOrCreateChunkState(idx) }
            }
        } else {
            null
        }
    val chunkStartLineProvider = remember(viewModel, uiState.contentVersion) {
        { idx: Int -> viewModel.getChunkStartLine(idx) }
    }
    val onChunkDisposed: ((Int) -> Unit)? = if (isEditable) {
        remember(viewModel) { { idx: Int -> viewModel.disposeChunkState(idx) } }
    } else {
        null
    }
    val focusedChunk = uiState.focusedEditChunk
    val isChunkReadOnly: (Int) -> Boolean = if (isEditable) {
        remember(focusedChunk) { { idx: Int -> abs(idx - focusedChunk) > 1 } }
    } else {
        remember { { _: Int -> true } }
    }
    val onChunkFocused: ((Int) -> Unit)? = if (isEditable) {
        remember(viewModel) { { idx: Int -> viewModel.setFocusedEditChunk(idx) } }
    } else {
        null
    }

    BackHandler {
        when {
            uiState.showDiscardDialog -> viewModel.dismissDiscardDialog()
            uiState.mode == TextEditorMode.Edit && viewModel.isContentDirty() ->
                viewModel.requestShowDiscardDialog()
            uiState.mode == TextEditorMode.Edit -> viewModel.setViewMode()
            uiState.mode == TextEditorMode.Create -> {
                pendingBackAfterSave = true
                viewModel.saveFile()
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
                        if (viewModel.isContentDirty())
                            viewModel.requestShowDiscardDialog()
                        else viewModel.setViewMode()
                    },
                    onSave = { viewModel.saveFile() },
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
                        message = uiState.errorMessage?.takeIf { it.isNotBlank() }
                            ?: stringResource(sharedR.string.general_request_failed_message),
                        onDismiss = {
                            viewModel.consumeErrorEvent()
                            onBack()
                        },
                    )
                }

                else -> {
                    TextEditorContent(
                        lazyListState = lazyListState,
                        chunkCount = viewModel.getChunkCount(),
                        totalLineCount = uiState.totalLineCount,
                        chunkTextProvider = chunkTextProvider,
                        chunkStateProvider = chunkStateProvider,
                        chunkStartLineProvider = chunkStartLineProvider,
                        onChunkDisposed = onChunkDisposed,
                        isChunkReadOnly = isChunkReadOnly,
                        onChunkFocused = onChunkFocused,
                        showLineNumbers = uiState.showLineNumbers,
                        readOnly = !isEditable,
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
                    if (action is TextEditorBottomBarAction.Edit) {
                        viewModel.setEditMode(lazyListState.firstVisibleItemIndex)
                    } else {
                        (action as? TextEditorBottomBarAction)?.let {
                            viewModel.onBottomBarAction(it)
                        }
                    }
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

private val LoadingContentTopPadding = 153.dp

@Composable
private fun TextEditorLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LoadingContentTopPadding),
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
    actions: List<TextEditorBottomBarAction>,
    onActionPressed: (TextEditorBottomBarAction) -> Unit,
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
                onActionPressed = { action ->
                    (action as? TextEditorBottomBarAction)?.let(onActionPressed)
                },
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
