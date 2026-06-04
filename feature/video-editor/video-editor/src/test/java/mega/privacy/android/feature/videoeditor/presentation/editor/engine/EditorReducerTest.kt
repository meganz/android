package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.PlaybackState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.SourceState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim.TrimState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume.VolumeState
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

@OptIn(UnstableApi::class)
class EditorReducerTest {

    private val uri = mock<Uri>()
    private val emptyRegistry = ToolRegistry(emptyList())
    private val registry = ToolRegistry(listOf(FakeTool))

    private fun loadedState(durationMs: Long = 1_000L): EditorState =
        reduce(
            reduce(EditorState(), EditorAction.LoadVideo(uri), registry),
            EditorAction.SourceLoaded(
                uri = uri,
                durationMs = durationMs,
                widthPx = 1920,
                heightPx = 1080
            ),
            registry,
        )

    @Test
    fun `test that LoadVideo resets state with the new source uri`() {
        val state = reduce(EditorState(), EditorAction.LoadVideo(uri), registry)

        assertThat(state.source.uri).isEqualTo(uri)
        assertThat(state.source.durationMs).isEqualTo(0L)
    }

    @Test
    fun `test that SourceLoaded sets duration dimensions and full trim range`() {
        val state = loadedState(durationMs = 5_000L)

        assertThat(state.source.durationMs).isEqualTo(5_000L)
        assertThat(state.source.widthPx).isEqualTo(1920)
        assertThat(state.trim).isEqualTo(TrimState(startMs = 0L, endMs = 5_000L))
    }

    @Test
    fun `test that SourceLoaded is ignored when its uri is not the active source`() {
        val loaded = reduce(EditorState(), EditorAction.LoadVideo(uri), registry)
        val staleUri = mock<Uri>()

        val state = reduce(
            loaded,
            EditorAction.SourceLoaded(
                uri = staleUri,
                durationMs = 9_000L,
                widthPx = 10,
                heightPx = 10
            ),
            registry,
        )

        assertThat(state.source.durationMs).isEqualTo(0L)
    }

    @Test
    fun `test that SetPlaying updates the playback slice`() {
        val state = reduce(EditorState(), EditorAction.SetPlaying(true), registry)

        assertThat(state.playback.isPlaying).isTrue()
    }

    @Test
    fun `test that SetPlayhead is clamped to the trim range`() {
        val loaded = loadedState(durationMs = 1_000L)

        val beyondEnd = reduce(loaded, EditorAction.SetPlayhead(5_000L), registry)
        val beforeStart = reduce(loaded, EditorAction.SetPlayhead(-10L), registry)

        assertThat(beyondEnd.playback.playheadMs).isEqualTo(1_000L)
        assertThat(beforeStart.playback.playheadMs).isEqualTo(0L)
    }

    @Test
    fun `test that ClearSource resets to the default state`() {
        // Assert per-slice rather than the whole EditorState: CropState holds an
        // android.graphics.RectF whose equals() is unreliable under the stubbed
        // android.jar in plain JVM unit tests.
        val state = reduce(loadedState(), EditorAction.ClearSource, registry)

        assertThat(state.source).isEqualTo(SourceState())
        assertThat(state.trim).isEqualTo(TrimState())
        assertThat(state.playback).isEqualTo(PlaybackState())
        assertThat(state.volume).isEqualTo(VolumeState())
        assertThat(state.activeTool).isNull()
        assertThat(state.toolSnapshot).isNull()
    }

    @Test
    fun `test that EnterTool with an unregistered tool leaves state unchanged`() {
        val state = reduce(EditorState(), EditorAction.EnterTool(FakeToolId), emptyRegistry)

        assertThat(state.activeTool).isNull()
        assertThat(state.toolSnapshot).isNull()
    }

    @Test
    fun `test that EnterTool activates the tool captures a snapshot and pauses`() {
        val playing = reduce(EditorState(), EditorAction.SetPlaying(true), registry)

        val state = reduce(playing, EditorAction.EnterTool(FakeToolId), registry)

        assertThat(state.activeTool).isEqualTo(FakeToolId)
        assertThat(state.toolSnapshot).isNotNull()
        assertThat(state.playback.isPlaying).isFalse()
    }

    @Test
    fun `test that DispatchTool routes the action to the active tool reducer`() {
        val active = reduce(EditorState(), EditorAction.EnterTool(FakeToolId), registry)

        val state = reduce(active, EditorAction.DispatchTool(FakeAction(volume = 0.5f)), registry)

        assertThat(state.volume).isEqualTo(VolumeState(0.5f))
    }

    @Test
    fun `test that CancelTool restores the snapshot and clears the active tool`() {
        val active = reduce(EditorState(), EditorAction.EnterTool(FakeToolId), registry)
        val edited = reduce(active, EditorAction.DispatchTool(FakeAction(volume = 0f)), registry)

        val state = reduce(edited, EditorAction.CancelTool, registry)

        assertThat(state.volume).isEqualTo(VolumeState())
        assertThat(state.activeTool).isNull()
        assertThat(state.toolSnapshot).isNull()
    }

    @Test
    fun `test that ApplyTool keeps the edit clears the tool and resumes`() {
        val active = reduce(EditorState(), EditorAction.EnterTool(FakeToolId), registry)
        val edited = reduce(active, EditorAction.DispatchTool(FakeAction(volume = 0.25f)), registry)

        val state = reduce(edited, EditorAction.ApplyTool, registry)

        assertThat(state.volume).isEqualTo(VolumeState(0.25f))
        assertThat(state.activeTool).isNull()
        assertThat(state.playback.isPlaying).isTrue()
    }

    @Test
    fun `test that ResetActiveTool resets the active tool slice`() {
        val active = reduce(EditorState(), EditorAction.EnterTool(FakeToolId), registry)
        val edited = reduce(active, EditorAction.DispatchTool(FakeAction(volume = 0.1f)), registry)

        val state = reduce(edited, EditorAction.ResetActiveTool, registry)

        assertThat(state.volume).isEqualTo(VolumeState())
    }

    private companion object {
        val FakeToolId = ToolId("fake")
    }

    private data class FakeAction(val volume: Float) : ToolAction

    /** A minimal tool that owns the volume slice, used to exercise tool routing. */
    @OptIn(UnstableApi::class)
    private object FakeTool : EditorTool {
        override val id = FakeToolId
        override val icon: ImageVector =
            ImageVector.Builder("fake", 24.dp, 24.dp, 24f, 24f).build()
        override val label = "Fake"

        override fun reduce(state: EditorState, action: ToolAction): EditorState =
            (action as? FakeAction)?.let { state.copy(volume = VolumeState(it.volume)) } ?: state

        override fun reset(state: EditorState): EditorState = state.copy(volume = VolumeState())

        override fun captureRollback(state: EditorState): ToolRollback {
            val saved = state.volume
            return ToolRollback { it.copy(volume = saved) }
        }

        override fun isApplied(state: EditorState): Boolean = !state.volume.isIdentity

        @Composable
        override fun Panel(
            state: EditorState,
            onAction: (ToolAction) -> Unit,
            modifier: Modifier,
        ) {
            // no-op: never rendered in reducer tests
        }
    }
}
