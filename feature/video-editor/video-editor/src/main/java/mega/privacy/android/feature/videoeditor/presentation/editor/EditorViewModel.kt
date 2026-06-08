package mega.privacy.android.feature.videoeditor.presentation.editor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import mega.privacy.android.feature.videoeditor.domain.usecase.GetVideoMetadataUseCase
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.reduce
import mega.privacy.android.feature.videoeditor.presentation.editor.export.ExportEvent
import mega.privacy.android.feature.videoeditor.presentation.editor.export.ExportProgress
import mega.privacy.android.feature.videoeditor.presentation.editor.export.VideoExporter
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import timber.log.Timber
import javax.inject.Inject

/**
 * Engine ViewModel for the editor's unidirectional MVI loop.
 *
 * Holds the immutable [EditorState] and drives it through the pure [reduce]
 * function against the injected [ToolRegistry]. It owns only the editing
 * concern — the source video is handed in by the host [mega.privacy.android.feature.videoeditor.presentation.screen.VideoEditorScreenViewModel]
 * via an [EditorAction.LoadVideo] dispatch and the export result is reported
 * back out, keeping all MEGA transfer/SDK concerns out of this class.
 *
 * On [EditorAction.LoadVideo] it reads the source metadata off the IO
 * dispatcher and feeds the result back as an [EditorAction.SourceLoaded].
 */
@OptIn(UnstableApi::class)
@HiltViewModel
internal class EditorViewModel @Inject constructor(
    val toolRegistry: ToolRegistry,
    private val getVideoMetadataUseCase: GetVideoMetadataUseCase,
    private val videoExporter: VideoExporter,
) : ViewModel() {

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress>(ExportProgress.Idle)
    val exportProgress: StateFlow<ExportProgress> = _exportProgress.asStateFlow()

    /** In-flight metadata read; cancelled when a new source is loaded or cleared. */
    private var metadataJob: Job? = null

    /**
     * The source's full metadata, retained from the load so [startExport] can
     * hand its GPS location to the exporter. Kept off [editorState] — it never
     * drives the UI. Null until a source is loaded, and cleared on
     * [EditorAction.ClearSource].
     */
    private var sourceMetadata: VideoMetadata? = null

    /** In-flight export; cancelled when a new export starts. */
    private var exportJob: Job? = null

    /** Dispatch a top-level editor action through the pure reducer. */
    fun dispatch(action: EditorAction) {
        _editorState.update { reduce(it, action, toolRegistry) }

        when (action) {
            is EditorAction.LoadVideo -> loadMetadata(action.uri)
            EditorAction.ClearSource -> {
                metadataJob?.cancel()
                sourceMetadata = null
            }

            else -> Unit
        }
    }

    private fun loadMetadata(uri: Uri) {
        // Drop any read still running for a previously-loaded source so its
        // result can't land after this one (the reducer also guards on URI).
        metadataJob?.cancel()
        sourceMetadata = null
        metadataJob = viewModelScope.launch {
            runCatching { getVideoMetadataUseCase(uri.toString()) }
                .onSuccess { info ->
                    sourceMetadata = info
                    dispatch(
                        EditorAction.SourceLoaded(
                            uri = uri,
                            durationMs = info.durationMs,
                            widthPx = info.widthPx,
                            heightPx = info.heightPx,
                        ),
                    )
                }
                .onFailure { Timber.e(it, "Failed to read metadata for $uri") }
        }
    }

    /**
     * Encode the current edit to a local file. No-op while an export is already
     * running or while the source isn't ready / the trim selection is empty.
     * Progress and the final result (or error) are surfaced via [exportProgress].
     */
    fun startExport() {
        if (_exportProgress.value is ExportProgress.InProgress) return
        val state = _editorState.value
        if (!state.source.isLoaded || state.trim.endMs <= state.trim.startMs) return

        _exportProgress.value = ExportProgress.InProgress(0)
        exportJob = videoExporter.export(state, toolRegistry.tools, sourceMetadata)
            .onEach { event ->
                _exportProgress.value = when (event) {
                    is ExportEvent.Progress -> ExportProgress.InProgress(event.percent)
                    is ExportEvent.Completed -> ExportProgress.Done(event.uri)
                }
            }
            .catch { error ->
                Timber.e(error, "Video export failed")
                _exportProgress.value = ExportProgress.Error(error.message ?: "Export failed")
            }
            .launchIn(viewModelScope)
    }

    /**
     * Abort an export in progress. Cancels the encode job — which stops the
     * underlying transformer on its finally path — and returns to idle.
     */
    fun cancelExport() {
        exportJob?.cancel()
        _exportProgress.value = ExportProgress.Idle
    }

    /** Clear a finished export's result so the editor can be used again. */
    fun dismissExportResult() {
        _exportProgress.value = ExportProgress.Idle
    }
}