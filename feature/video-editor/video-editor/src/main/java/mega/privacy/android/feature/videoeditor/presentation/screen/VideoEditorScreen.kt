package mega.privacy.android.feature.videoeditor.presentation.screen

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.PrimaryFilledButtonM3XSmall
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.videoeditor.components.ToolTabBar
import mega.privacy.android.feature.videoeditor.components.ToolTabUiItem
import mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.export.ExportProgress
import mega.privacy.android.feature.videoeditor.presentation.editor.render.EditorPreview
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.EditorErrorState
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.ExportProgressDialog
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.PrepareVideoDialog
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.PreparingPreview
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.PreviewControls
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.PreviewStatusBadges
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.ToolActionBar
import mega.privacy.android.feature.videoeditor.presentation.editor.ui.ToolDeck
import mega.privacy.android.feature.videoeditor.presentation.screen.model.VideoEditorUiState
import java.io.File

/**
 * Stateful entry point for the video editor.
 *
 * Hosts the [VideoEditorScreenViewModel] (download) and the [EditorViewModel]
 * (editing) for the given [nodeHandle], and bridges them: once the source video
 * is downloaded, its file is handed to the editor via [EditorAction.LoadVideo].
 *
 * @param nodeHandle The MEGA node handle of the video to edit.
 * @param onClose Invoked when the user dismisses the editor.
 * @param onTransfer Invoked with the upload event for the exported copy; the host wires it to the
 * app's transfer subsystem.
 */
@UnstableApi
@Composable
internal fun VideoEditorRoute(
    nodeHandle: Long,
    onClose: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val screenViewModel =
        hiltViewModel<VideoEditorScreenViewModel, VideoEditorScreenViewModel.Factory> { factory ->
            factory.create(nodeHandle)
        }

    val editorViewModel = hiltViewModel<EditorViewModel>()

    val uiState by screenViewModel.uiState.collectAsStateWithLifecycle()
    val editorState by editorViewModel.editorState.collectAsStateWithLifecycle()
    val exportProgress by editorViewModel.exportProgress.collectAsStateWithLifecycle()

    // Hand the upload event to the app's transfer subsystem, then close the editor. The app (still
    // alive, the editor is just a nav entry) enqueues the upload and shows its start snackbar.
    EventEffect(
        event = uiState.transferEvent,
        onConsumed = screenViewModel::consumeTransferEvent,
    ) { event ->
        onTransfer(event)
        onClose()
    }

    // Hand the downloaded file to the editor exactly once, when it becomes ready.
    LaunchedEffect(uiState.videoFilePath) {
        val path = uiState.videoFilePath
        if (path != null && editorState.source.uri == null) {
            editorViewModel.dispatch(EditorAction.LoadVideo(Uri.fromFile(File(path))))
        }
    }

    // Only react to the terminal outcome, not every progress tick.
    val exportResult by remember {
        derivedStateOf {
            exportProgress as? ExportProgress.Done ?: exportProgress as? ExportProgress.Error
        }
    }

    // Relay the export result to the host (owns the MEGA side).
    LaunchedEffect(exportResult) {
        when (val result = exportResult) {
            is ExportProgress.Done -> {
                screenViewModel.onExportSucceeded(result.outputUri)
                editorViewModel.dismissExportResult()
            }

            is ExportProgress.Error -> {
                screenViewModel.onExportFailed()
                editorViewModel.dismissExportResult()
            }

            else -> Unit
        }
    }

    // State to enabled Save button
    val saveEnabled by remember {
        derivedStateOf {
            editorState.source.isLoaded &&
                    editorViewModel.toolRegistry.tools.any { it.isApplied(editorState) } &&
                    exportProgress !is ExportProgress.InProgress &&
                    exportProgress !is ExportProgress.Done
        }
    }

    VideoEditorScreen(
        uiState = uiState,
        editorState = editorState,
        exportProgress = exportProgress,
        registry = editorViewModel.toolRegistry,
        saveEnabled = saveEnabled,
        onAction = editorViewModel::dispatch,
        onSave = editorViewModel::startExport,
        onCancelExport = editorViewModel::cancelExport,
        onCancelDownload = {
            screenViewModel.cancelDownload()
            onClose()
        },
        onClose = onClose,
    )
}

/**
 * Stateless video editor screen. Shows the download progress while the source
 * is fetched into the cache, then the editor once it is ready. All themed,
 * token-using chrome is delegated to components in the snowflakes module.
 */
@UnstableApi
@Composable
internal fun VideoEditorScreen(
    uiState: VideoEditorUiState,
    editorState: EditorState,
    exportProgress: ExportProgress,
    registry: ToolRegistry,
    saveEnabled: Boolean,
    onAction: (EditorAction) -> Unit,
    onSave: () -> Unit,
    onCancelExport: () -> Unit,
    onCancelDownload: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isError = uiState.isError || editorState.source.loadFailed

    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            // Slides away while a tool is active so the preview gets the full
            // height; always visible in the error state so the user can leave.
            AnimatedVisibility(
                visible = isError || editorState.activeTool == null,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
                ),
            ) {
                MegaTopAppBar(
                    title = "Edit video",
                    navigationType = AppBarNavigationType.Close(onClose),
                    trailingIcons = {
                        if (!isError) {
                            PrimaryFilledButtonM3XSmall(
                                modifier = Modifier.padding(end = 16.dp),
                                text = "Save copy",
                                onClick = onSave,
                                enabled = saveEnabled
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        if (isError) {
            EditorErrorState(
                message = "Failed to load video", // TODO string resource
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            EditorBody(
                state = editorState,
                exportProgress = exportProgress,
                preparingImagePath = uiState.previewImagePath,
                exportFileName = uiState.exportFileName,
                registry = registry,
                onAction = onAction,
                onCancelExport = onCancelExport,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            if (uiState.isDownloading) {
                PrepareVideoDialog(
                    fileName = uiState.fileName,
                    fileSizeBytes = uiState.fileSizeBytes,
                    percent = uiState.downloadProgress,
                    onCancel = onCancelDownload,
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun EditorBody(
    state: EditorState,
    exportProgress: ExportProgress,
    preparingImagePath: String?,
    exportFileName: String,
    registry: ToolRegistry,
    onAction: (EditorAction) -> Unit,
    onCancelExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            var isBuffering by remember { mutableStateOf(false) }
            EditorPreview(
                state = state,
                registry = registry,
                onAction = onAction,
                onBufferingChange = { isBuffering = it },
                modifier = Modifier.fillMaxSize(),
            )
            // Until the downloaded video is loaded into the player, show its preview/thumbnail still
            if (!state.source.isLoaded && preparingImagePath != null) {
                PreparingPreview(
                    imagePath = preparingImagePath,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            PreviewStatusBadges(
                speed = state.speed.speed,
                showSpeed = !state.speed.isIdentity && state.activeTool != BuiltInToolIds.Speed,
                showMute = state.volume.isMuted && state.activeTool != BuiltInToolIds.Volume,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            PreviewControls(
                isPlaying = state.playback.isPlaying,
                playheadMs = state.playback.playheadMs,
                trimStartMs = state.trim.startMs,
                trimEndMs = state.trim.endMs,
                hideScrub = state.activeTool == BuiltInToolIds.Trim,
                isBuffering = isBuffering,
                onPlayPause = { onAction(EditorAction.SetPlaying(!state.playback.isPlaying)) },
                onSeek = { onAction(EditorAction.SetPlayhead(it)) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        BottomSlot(state = state, registry = registry, onAction = onAction)
    }

    (exportProgress as? ExportProgress.InProgress)?.let { inProgress ->
        ExportProgressDialog(
            percent = inProgress.percent,
            fileName = exportFileName,
            onCancel = onCancelExport,
        )
    }
}

@UnstableApi
@Composable
private fun BottomSlot(
    state: EditorState,
    registry: ToolRegistry,
    onAction: (EditorAction) -> Unit,
) {
    // Hold the last-active tool so the deck keeps rendering its content while the exit-slide
    // plays; state.activeTool flips to null on dismiss, which would otherwise empty the deck.
    var lastTool by remember { mutableStateOf<ToolId?>(null) }
    LaunchedEffect(state.activeTool) {
        if (state.activeTool != null) lastTool = state.activeTool
    }
    val renderTool = state.activeTool ?: lastTool

    val tabs = registry.tools.map { tool ->
        ToolTabUiItem(
            id = tool.id.value,
            icon = tool.icon,
            label = tool.label,
            selected = state.activeTool == tool.id,
            applied = tool.isApplied(state),
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ToolTabBar(
            items = tabs,
            onSelect = { onAction(EditorAction.EnterTool(ToolId(it))) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = state.activeTool != null,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
                initialOffsetY = { it },
            ) + expandVertically(
                animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
                expandFrom = Alignment.Bottom,
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                targetOffsetY = { it },
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                shrinkTowards = Alignment.Bottom,
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            ) {
                renderTool?.let { id ->
                    registry[id]?.let { tool ->
                        ToolDeck {
                            tool.Panel(
                                state = state,
                                onAction = { onAction(EditorAction.DispatchTool(it)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        ToolActionBar(
                            toolLabel = tool.label,
                            onCancel = { onAction(EditorAction.CancelTool) },
                            onApply = { onAction(EditorAction.ApplyTool) },
                        )
                    }
                }
            }
        }
    }
}
