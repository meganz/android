package mega.privacy.android.feature.videoeditor.presentation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import mega.privacy.android.feature.videoeditor.domain.usecase.GetVideoMetadataUseCase
import mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.export.ExportEvent
import mega.privacy.android.feature.videoeditor.presentation.editor.export.ExportProgress
import mega.privacy.android.feature.videoeditor.presentation.editor.export.VideoExporter
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
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
    private val videoExporter = mock<VideoExporter>()

    @BeforeEach
    fun resetMocks() {
        reset(getVideoMetadataUseCase, videoExporter)
    }

    private fun initViewModel() = EditorViewModel(
        toolRegistry = ToolRegistry(emptyList()),
        getVideoMetadataUseCase = getVideoMetadataUseCase,
        videoExporter = videoExporter,
    )

    /** Drives the VM into a loaded-source state so an export can be started. */
    private fun loadedViewModel(uri: Uri): EditorViewModel {
        wheneverBlocking { getVideoMetadataUseCase(any()) } doReturn VideoMetadata(
            durationMs = 5_000L,
            widthPx = 1920,
            heightPx = 1080,
        )
        return initViewModel().apply { dispatch(EditorAction.LoadVideo(uri)) }
    }

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

    @Test
    fun `test that LoadVideo marks the source as failed when the metadata has no duration`() =
        runTest {
            val uri = mock<Uri>()
            wheneverBlocking { getVideoMetadataUseCase(any()) } doReturn VideoMetadata(
                durationMs = 0L,
                widthPx = 0,
                heightPx = 0,
            )
            val underTest = initViewModel()

            underTest.dispatch(EditorAction.LoadVideo(uri))
            advanceUntilIdle()

            val state = underTest.editorState.value
            assertThat(state.source.loadFailed).isTrue()
            assertThat(state.source.isLoaded).isFalse()
        }

    @Test
    fun `test that LoadVideo marks the source as failed when the metadata read throws`() =
        runTest {
            val uri = mock<Uri>()
            wheneverBlocking { getVideoMetadataUseCase(any()) } doAnswer {
                throw RuntimeException("boom")
            }
            val underTest = initViewModel()

            underTest.dispatch(EditorAction.LoadVideo(uri))
            advanceUntilIdle()

            assertThat(underTest.editorState.value.source.loadFailed).isTrue()
        }

    @Test
    fun `test that startExport emits Done with the exported uri on success`() = runTest {
        val outputUri = mock<Uri>()
        whenever(
            videoExporter.export(
                any(),
                any(),
                anyOrNull()
            )
        ) doReturn flowOf(ExportEvent.Completed(outputUri))
        val underTest = loadedViewModel(mock())
        advanceUntilIdle()

        underTest.startExport()
        advanceUntilIdle()

        assertThat(underTest.exportProgress.value).isEqualTo(ExportProgress.Done(outputUri))
    }

    @Test
    fun `test that startExport surfaces progress events as InProgress`() = runTest {
        whenever(
            videoExporter.export(
                any(),
                any(),
                anyOrNull()
            )
        ) doReturn flowOf(ExportEvent.Progress(42))
        val underTest = loadedViewModel(mock())
        advanceUntilIdle()

        underTest.startExport()
        advanceUntilIdle()

        assertThat(underTest.exportProgress.value).isEqualTo(ExportProgress.InProgress(42))
    }

    @Test
    fun `test that startExport emits Error when the export fails`() = runTest {
        whenever(
            videoExporter.export(
                any(),
                any(),
                anyOrNull()
            )
        ) doReturn flow { throw RuntimeException("boom") }
        val underTest = loadedViewModel(mock())
        advanceUntilIdle()

        underTest.startExport()
        advanceUntilIdle()

        assertThat(underTest.exportProgress.value).isEqualTo(ExportProgress.Error("boom"))
    }

    @Test
    fun `test that startExport passes the loaded source metadata to the exporter`() = runTest {
        val metadata = VideoMetadata(
            durationMs = 5_000L,
            widthPx = 1920,
            heightPx = 1080,
            latitude = 1.5f,
            longitude = 2.5f,
        )
        wheneverBlocking { getVideoMetadataUseCase(any()) } doReturn metadata
        whenever(
            videoExporter.export(
                any(),
                any(),
                anyOrNull()
            )
        ) doReturn flowOf(ExportEvent.Completed(mock()))
        val underTest = initViewModel().apply { dispatch(EditorAction.LoadVideo(mock())) }
        advanceUntilIdle()

        underTest.startExport()
        advanceUntilIdle()

        verify(videoExporter).export(any(), any(), eq(metadata))
    }

    @Test
    fun `test that startExport does nothing when the source is not loaded`() = runTest {
        val underTest = initViewModel()

        underTest.startExport()
        advanceUntilIdle()

        assertThat(underTest.exportProgress.value).isEqualTo(ExportProgress.Idle)
        verifyNoInteractions(videoExporter)
    }

    @Test
    fun `test that dismissExportResult resets the export progress to idle`() = runTest {
        val outputUri = mock<Uri>()
        whenever(
            videoExporter.export(
                any(),
                any(),
                anyOrNull()
            )
        ) doReturn flowOf(ExportEvent.Completed(outputUri))
        val underTest = loadedViewModel(mock())
        advanceUntilIdle()
        underTest.startExport()
        advanceUntilIdle()

        underTest.dismissExportResult()

        assertThat(underTest.exportProgress.value).isEqualTo(ExportProgress.Idle)
    }
}
