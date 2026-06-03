package mega.privacy.android.feature.videoeditor.presentation.editor

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.reduce
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import javax.inject.Inject

/**
 * Engine ViewModel for the editor's unidirectional MVI loop.
 *
 * Holds the immutable [EditorState] and drives it through the pure [reduce]
 * function against the injected [ToolRegistry]. It owns only the editing
 * concern — the source video is handed in by the host [mega.privacy.android.feature.videoeditor.presentation.screen.VideoEditorScreenViewModel]
 * via an [EditorAction.LoadVideo] dispatch and the export result is reported
 * back out, keeping all MEGA transfer/SDK concerns out of this class.
 */
@OptIn(UnstableApi::class)
@HiltViewModel
internal class EditorViewModel @Inject constructor(
    private val toolRegistry: ToolRegistry,
) : ViewModel() {

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    /** Dispatch a top-level editor action through the pure reducer. */
    fun dispatch(action: EditorAction) {
        _editorState.update { reduce(it, action, toolRegistry) }
    }
}