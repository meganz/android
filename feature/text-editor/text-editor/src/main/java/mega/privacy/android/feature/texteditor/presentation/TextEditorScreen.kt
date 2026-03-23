package mega.privacy.android.feature.texteditor.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

/** Epsilon in px for float comparison when deciding if top bar is fully hidden. */
private const val BARS_HIDDEN_EPSILON_PX = 1f

private const val REVEAL_ANIMATION_MS = 200
private const val ENTRANCE_ANIMATION_MS = 300
private const val FLING_SNAP_ANIMATION_MS = 150
private val BottomBarSlideDistance = 100.dp

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
    val scope = rememberCoroutineScope()

    val scrollBarState = rememberScrollToHideBarState(uiState.mode, scope)
    val barsHidden by scrollBarState.barsHidden
    val bottomBarTranslationY by scrollBarState.bottomBarTranslationY
    val bottomBarEntranceOffset = scrollBarState.bottomBarEntranceOffset
    val bottomBarSlideDistancePx = scrollBarState.bottomBarSlideDistancePx

    val chunkCount = remember(uiState.totalLineCount, uiState.contentVersion) {
        viewModel.getChunkCount()
    }
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
            barsHidden -> scrollBarState.revealBar()
            uiState.showDiscardDialog -> viewModel.dismissDiscardDialog()
            uiState.mode == TextEditorMode.Edit && viewModel.isContentDirty() ->
                viewModel.requestShowDiscardDialog()
            uiState.mode == TextEditorMode.Edit ->
                if (viewModel.shouldPopDestinationOnCleanEditExit()) onBack()
                else viewModel.setViewMode()
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
            CollapsingTopBar(
                scrollBarState = scrollBarState,
                mode = uiState.mode,
                fileName = uiState.fileName,
                scope = scope,
                lazyListState = lazyListState,
                viewModel = viewModel,
                onSetPendingBack = { pendingBackAfterSave = true },
                onBack = onBack,
                onOpenNodeOptions = onOpenNodeOptions,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBarState.scrollConnection)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = barsHidden,
                ) { scrollBarState.revealBar() },
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TextEditorLoadingContent()
                    }
                }

                uiState.errorEvent == triggered -> {
                    SideEffect { pendingBackAfterSave = false }
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
                        chunkCount = chunkCount,
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
            val showBottomBar = uiState.mode != TextEditorMode.Edit
                    && uiState.bottomBarActions.isNotEmpty()
                    && !uiState.isLoading
            LaunchedEffect(showBottomBar) {
                if (showBottomBar) {
                    bottomBarEntranceOffset.snapTo(bottomBarSlideDistancePx)
                    bottomBarEntranceOffset.animateTo(0f, animationSpec = tween(ENTRANCE_ANIMATION_MS))
                } else {
                    bottomBarEntranceOffset.snapTo(bottomBarSlideDistancePx)
                }
            }
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                        .graphicsLayer {
                            translationY =
                                bottomBarEntranceOffset.value + bottomBarTranslationY
                        },
                ) {
                    MegaFloatingToolbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        actions = uiState.bottomBarActions,
                        actionsEnabled = true,
                        onActionPressed = { action ->
                            when (action) {
                                is TextEditorBottomBarAction.Edit ->
                                    viewModel.setEditMode(lazyListState.firstVisibleItemIndex)
                                is TextEditorBottomBarAction ->
                                    viewModel.onBottomBarAction(action)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Top bar that collapses upward as the user scrolls down, and reveals when
 * scrolling back up or tapping. Uses a custom [Layout] for pixel-precise offset.
 */
@Composable
private fun CollapsingTopBar(
    scrollBarState: ScrollToHideBarState,
    mode: TextEditorMode,
    fileName: String,
    scope: CoroutineScope,
    lazyListState: LazyListState,
    viewModel: TextEditorComposeViewModel,
    onSetPendingBack: () -> Unit,
    onBack: () -> Unit,
    onOpenNodeOptions: () -> Unit,
) {
    Layout(
        content = {
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    scrollBarState.topBarHeightPx = coordinates.size.height.toFloat()
                },
            ) {
                val onTitleClick: () -> Unit = remember(scope, lazyListState, scrollBarState) {
                    {
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                            scrollBarState.topBarOffsetPx = 0f
                        }
                        Unit
                    }
                }
                val onClose = remember(viewModel, onBack) {
                    {
                        if (viewModel.isContentDirty()) {
                            viewModel.requestShowDiscardDialog()
                        } else if (viewModel.shouldPopDestinationOnCleanEditExit()) {
                            onBack()
                        } else {
                            viewModel.setViewMode()
                        }
                    }
                }
                val onSave = remember(viewModel) {
                    {
                        onSetPendingBack()
                        viewModel.saveFile()
                    }
                }
                when (mode) {
                    TextEditorMode.Edit -> TextEditorEditModeTopAppBar(
                        title = fileName,
                        onClose = onClose,
                        onSave = onSave,
                        onMenuAction = viewModel::onMenuAction,
                        onTitleClick = onTitleClick,
                    )

                    else -> TextEditorViewModeTopAppBar(
                        title = fileName,
                        onBack = onBack,
                        onMenuAction = viewModel::onMenuAction,
                        onOpenNodeOptions = onOpenNodeOptions,
                        onTitleClick = onTitleClick,
                    )
                }
            }
        },
        modifier = Modifier.clipToBounds(),
    ) { measurables, constraints ->
        val placeable = measurables[0].measure(constraints.copy(minHeight = 0))
        val visibleHeight =
            (placeable.height + scrollBarState.topBarOffsetPx.toInt()).coerceAtLeast(0)
        layout(constraints.maxWidth, visibleHeight) {
            placeable.placeRelative(0, scrollBarState.topBarOffsetPx.toInt())
        }
    }
}

/**
 * True when the top bar is considered fully hidden (for back/tap-to-reveal).
 * Exposed for unit testing the threshold logic.
 */
internal fun isBarsHidden(
    topBarHeightPx: Float,
    topBarOffsetPx: Float,
    epsilon: Float = BARS_HIDDEN_EPSILON_PX,
): Boolean = topBarHeightPx > 0f && topBarOffsetPx <= -topBarHeightPx + epsilon

/**
 * State and connection for scroll-to-hide top bar and linked bottom bar.
 * Use [rememberScrollToHideBarState] to create.
 */
internal class ScrollToHideBarState(
    private val topBarHeightPxState: MutableFloatState,
    private val topBarOffsetPxState: MutableFloatState,
    val barsHidden: State<Boolean>,
    val bottomBarSlideDistancePx: Float,
    val bottomBarTranslationY: State<Float>,
    val bottomBarEntranceOffset: Animatable<Float, AnimationVector1D>,
    val scrollConnection: NestedScrollConnection,
    private val scope: CoroutineScope,
) {
    private var revealJob: Job? = null

    var topBarHeightPx: Float
        get() = topBarHeightPxState.floatValue
        set(value) {
            topBarHeightPxState.floatValue = value
        }

    var topBarOffsetPx: Float
        get() = topBarOffsetPxState.floatValue
        set(value) {
            topBarOffsetPxState.floatValue = value
        }

    fun revealBar() {
        revealJob?.cancel()
        revealJob = scope.launch {
            try {
                animate(
                    topBarOffsetPxState.floatValue,
                    0f,
                    animationSpec = tween(REVEAL_ANIMATION_MS),
                ) { v, _ ->
                    topBarOffsetPxState.floatValue = v
                }
            } finally {
                if (revealJob === coroutineContext[Job]) {
                    revealJob = null
                }
            }
        }
    }
}

@Composable
private fun rememberScrollToHideBarState(
    mode: TextEditorMode,
    scope: CoroutineScope,
): ScrollToHideBarState {
    val density = LocalDensity.current
    val topBarHeightPxState = remember { mutableFloatStateOf(0f) }
    val topBarOffsetPxState = remember { mutableFloatStateOf(0f) }
    val bottomBarSlideDistancePx = remember(density) { with(density) { BottomBarSlideDistance.toPx() } }
    val barsHiddenState: State<Boolean> = remember {
        derivedStateOf {
            isBarsHidden(
                topBarHeightPxState.floatValue,
                topBarOffsetPxState.floatValue,
            )
        }
    }
    val bottomBarTranslationYState: State<Float> = remember {
        derivedStateOf {
            val h = topBarHeightPxState.floatValue
            val o = topBarOffsetPxState.floatValue
            if (h > 0f) (-o / h).coerceIn(0f, 1f) * bottomBarSlideDistancePx else 0f
        }
    }
    val bottomBarEntranceOffset = remember(density) {
        Animatable(bottomBarSlideDistancePx)
    }
    LaunchedEffect(mode) { topBarOffsetPxState.floatValue = 0f }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && topBarHeightPxState.floatValue > 0f) {
                    topBarOffsetPxState.floatValue =
                        (topBarOffsetPxState.floatValue + available.y)
                            .coerceIn(-topBarHeightPxState.floatValue, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y > 0f && topBarHeightPxState.floatValue > 0f) {
                    topBarOffsetPxState.floatValue =
                        (topBarOffsetPxState.floatValue + consumed.y)
                            .coerceIn(-topBarHeightPxState.floatValue, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                val height = topBarHeightPxState.floatValue
                val offset = topBarOffsetPxState.floatValue
                if (height > 0f && offset > -height && offset < 0f) {
                    val target = if (offset > -height / 2) 0f else -height
                    animate(offset, target, animationSpec = tween(FLING_SNAP_ANIMATION_MS)) { v, _ ->
                        topBarOffsetPxState.floatValue = v
                    }
                }
                return Velocity.Zero
            }
        }
    }
    return remember(scope) {
        ScrollToHideBarState(
            topBarHeightPxState = topBarHeightPxState,
            topBarOffsetPxState = topBarOffsetPxState,
            barsHidden = barsHiddenState,
            bottomBarSlideDistancePx = bottomBarSlideDistancePx,
            bottomBarTranslationY = bottomBarTranslationYState,
            bottomBarEntranceOffset = bottomBarEntranceOffset,
            scrollConnection = scrollConnection,
            scope = scope,
        )
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

/**
 * Edit-mode top app bar. [onTitleClick] scrolls to top and reveals the bar; action buttons
 * (Save, Line numbers) use their own hit targets and are not affected.
 */
@Composable
private fun TextEditorEditModeTopAppBar(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
    onTitleClick: () -> Unit,
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
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTitleClick,
        ),
    )
}

/**
 * View-mode top app bar. [onTitleClick] scrolls to top and reveals the bar; action buttons
 * (More, Line numbers) use their own hit targets and are not affected.
 */
@Composable
private fun TextEditorViewModeTopAppBar(
    title: String,
    onBack: () -> Unit,
    onMenuAction: (TextEditorTopBarAction) -> Unit,
    onOpenNodeOptions: () -> Unit,
    onTitleClick: () -> Unit,
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
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTitleClick,
        ),
    )
}
