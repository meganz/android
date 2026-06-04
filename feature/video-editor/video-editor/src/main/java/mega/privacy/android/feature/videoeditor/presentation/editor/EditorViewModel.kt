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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.videoeditor.domain.usecase.GetVideoMetadataUseCase
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.reduce
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
) : ViewModel() {

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    /** In-flight metadata read; cancelled when a new source is loaded or cleared. */
    private var metadataJob: Job? = null

    /** Dispatch a top-level editor action through the pure reducer. */
    fun dispatch(action: EditorAction) {
        _editorState.update { reduce(it, action, toolRegistry) }

        when (action) {
            is EditorAction.LoadVideo -> loadMetadata(action.uri)
            EditorAction.ClearSource -> metadataJob?.cancel()
            else -> Unit
        }
    }

    private fun loadMetadata(uri: Uri) {
        // Drop any read still running for a previously-loaded source so its
        // result can't land after this one (the reducer also guards on URI).
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            runCatching { getVideoMetadataUseCase(uri.toString()) }
                .onSuccess { info ->
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
}