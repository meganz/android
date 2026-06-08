package mega.privacy.android.feature.videoeditor.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import android.net.Uri
import mega.privacy.android.feature.videoeditor.presentation.screen.VideoEditorScreenViewModel
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoEditorScreenViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())

        private const val NODE_HANDLE = 12345L
        private const val FILE_NAME = "video.mp4"
        private const val FILE_SIZE = 100L
    }

    private lateinit var underTest: VideoEditorScreenViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val downloadNodeUseCase = mock<DownloadNodeUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeEach
    fun setUp() {
        reset(
            getNodeByIdUseCase,
            getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase,
            snackbarEventQueue,
        )
    }

    private fun initViewModel() {
        underTest = VideoEditorScreenViewModel(
            nodeHandle = NODE_HANDLE,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase = downloadNodeUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    private fun stubNode(): TypedFileNode = mock {
        on { name } doReturn FILE_NAME
        on { size } doReturn FILE_SIZE
    }

    private fun transferWithProgress(floatValue: Float): Transfer = mock {
        on { progress } doReturn Progress(floatValue)
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn null
        initViewModel()

        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.isError).isFalse()
        assertThat(underTest.uiState.value.videoFilePath).isNull()
    }

    @Test
    fun `test that state is error when node cannot be resolved`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn null
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
            assertThat(state.isError).isTrue()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that state is ready when a complete cached copy already exists`(
        @TempDir tempDir: File,
    ) = runTest {
        val cached = File(tempDir, FILE_NAME).apply { writeBytes(ByteArray(FILE_SIZE.toInt())) }
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
            assertThat(state.videoFilePath).isEqualTo(cached.path)
            assertThat(state.downloadProgress).isEqualTo(100)
            assertThat(state.isError).isFalse()
        }
        // Cached copy must not be downloaded again.
        org.mockito.kotlin.verifyNoInteractions(downloadNodeUseCase)
    }

    @Test
    fun `test that progress updates and state is ready when download succeeds`(
        @TempDir tempDir: File,
    ) = runTest {
        // Built outside the whenever stubbing to avoid nested stubbing.
        val updateEvent = TransferEvent.TransferUpdateEvent(transferWithProgress(0.5f))
        val finishEvent = TransferEvent.TransferFinishEvent(transferWithProgress(1f), error = null)
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flow {
            emit(updateEvent)
            File(tempDir, FILE_NAME).writeBytes(ByteArray(FILE_SIZE.toInt()))
            emit(finishEvent)
        }
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.videoFilePath == null && !state.isError) {
                state = awaitItem()
            }
            assertThat(state.videoFilePath).isEqualTo(File(tempDir, FILE_NAME).path)
            assertThat(state.downloadProgress).isEqualTo(100)
            assertThat(state.isLoading).isFalse()
            assertThat(state.isError).isFalse()
        }
    }

    @Test
    fun `test that state is error when download finishes with an error`(
        @TempDir tempDir: File,
    ) = runTest {
        // Built outside the whenever stubbing to avoid nested stubbing.
        val transfer = transferWithProgress(0f)
        val error = mock<mega.privacy.android.domain.exception.MegaException>()
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, error = error)
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flowOf(finishEvent)
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.isLoading) {
                state = awaitItem()
            }
            assertThat(state.isError).isTrue()
            assertThat(state.videoFilePath).isNull()
        }
    }

    @Test
    fun `test that onExportSucceeded queues a success message`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn null
        initViewModel()

        underTest.onExportSucceeded(mock<Uri>())
        advanceUntilIdle()

        verifyBlocking(snackbarEventQueue) { queueMessage(eq("Video saved")) }
    }

    @Test
    fun `test that onExportFailed queues a failure message`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn null
        initViewModel()

        underTest.onExportFailed()
        advanceUntilIdle()

        verifyBlocking(snackbarEventQueue) { queueMessage(eq("Couldn't save video")) }
    }
}
