package mega.privacy.android.feature.videoeditor.presentation

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
class EditorViewModelTest {

    private fun initViewModel() = EditorViewModel(toolRegistry = ToolRegistry(emptyList()))

    @Test
    fun `test that initial editor state is the default state`() {
        val underTest = initViewModel()

        assertThat(underTest.editorState.value.source.uri).isNull()
        assertThat(underTest.editorState.value.activeTool).isNull()
    }

    @Test
    fun `test that dispatch routes the action through the reducer`() {
        val underTest = initViewModel()

        underTest.dispatch(EditorAction.SetPlaying(true))

        assertThat(underTest.editorState.value.playback.isPlaying).isTrue()
    }
}
