package mega.privacy.android.feature.videoeditor.presentation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import mega.privacy.android.feature.videoeditor.domain.usecase.GetVideoMetadataUseCase
import mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditorViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }

    private val getVideoMetadataUseCase = mock<GetVideoMetadataUseCase>()

    private fun initViewModel() = EditorViewModel(
        toolRegistry = ToolRegistry(emptyList()),
        getVideoMetadataUseCase = getVideoMetadataUseCase,
    )

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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that LoadVideo reads metadata and emits SourceLoaded`() = runTest {
        val uri = mock<Uri>()
        wheneverBlocking { getVideoMetadataUseCase(any()) } doReturn VideoMetadata(
            durationMs = 5_000L,
            widthPx = 1920,
            heightPx = 1080,
        )
        val underTest = initViewModel()

        underTest.dispatch(EditorAction.LoadVideo(uri))
        advanceUntilIdle()

        val state = underTest.editorState.value
        assertThat(state.source.uri).isEqualTo(uri)
        assertThat(state.source.durationMs).isEqualTo(5_000L)
        assertThat(state.source.widthPx).isEqualTo(1920)
        assertThat(state.source.heightPx).isEqualTo(1080)
        assertThat(state.trim.endMs).isEqualTo(5_000L)
    }
}
