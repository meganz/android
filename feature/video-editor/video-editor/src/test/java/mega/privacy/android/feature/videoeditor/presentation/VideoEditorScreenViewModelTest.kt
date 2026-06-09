package mega.privacy.android.feature.videoeditor.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import android.content.Context
import android.net.Uri
import de.palm.composestateevents.StateEventWithContentTriggered
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import mega.privacy.android.feature.videoeditor.presentation.screen.VideoEditorScreenViewModel
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
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
import org.mockito.kotlin.wheneverBlocking
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoEditorScreenViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())

        private const val NODE_HANDLE = 12345L
        private const val PARENT_HANDLE = 67890L
        private const val FILE_NAME = "video.mp4"
        private const val FILE_SIZE = 100L
        private const val MODIFICATION_TIME = 1_700_000_000L
        private const val UPLOAD_MESSAGE = "Uploading edits as a new file"
    }

    private lateinit var underTest: VideoEditorScreenViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val downloadNodeUseCase = mock<DownloadNodeUseCase>()
    private val getNodeNameCollisionRenameNameUseCase = mock<GetNodeNameCollisionRenameNameUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val context = mock<Context> {
        on { getString(sharedR.string.photo_editor_upload_message) } doReturn UPLOAD_MESSAGE
    }

    @BeforeEach
    fun setUp() {
        reset(
            getNodeByIdUseCase,
            getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase,
            getNodeNameCollisionRenameNameUseCase,
            snackbarEventQueue,
        )
    }

    private fun initViewModel() {
        underTest = VideoEditorScreenViewModel(
            nodeHandle = NODE_HANDLE,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase = downloadNodeUseCase,
            getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase,
            snackbarEventQueue = snackbarEventQueue,
            context = context,
        )
    }

    private fun stubNode(): TypedFileNode = mock {
        on { id } doReturn NodeId(NODE_HANDLE)
        on { name } doReturn FILE_NAME
        on { size } doReturn FILE_SIZE
        on { parentId } doReturn NodeId(PARENT_HANDLE)
        on { modificationTime } doReturn MODIFICATION_TIME
    }

    /** Drives the VM through a ready cached download so [sourceNode] is resolved. */
    private suspend fun initReadyViewModel(tempDir: File) {
        File(tempDir, FILE_NAME).writeBytes(ByteArray(FILE_SIZE.toInt()))
        // Build the node outside the whenever stubbing to avoid nested stubbing.
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        initViewModel()
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
    fun `test that onExportSucceeded emits a StartUpload event to the source's parent folder`(
        @TempDir tempDir: File,
    ) = runTest {
        initReadyViewModel(tempDir)
        advanceUntilIdle()
        wheneverBlocking { getNodeNameCollisionRenameNameUseCase(any()) } doReturn "video (1).mp4"
        val outputUri = mock<Uri> { on { path } doReturn "/cache/video-editor-export.mp4" }

        underTest.onExportSucceeded(outputUri)
        advanceUntilIdle()

        val event = underTest.uiState.value.transferEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val content = (event as StateEventWithContentTriggered).content
        assertThat(content).isInstanceOf(TransferTriggerEvent.StartUpload.Files::class.java)
        val upload = content as TransferTriggerEvent.StartUpload.Files
        assertThat(upload.destinationId).isEqualTo(NodeId(PARENT_HANDLE))
        assertThat(upload.pathsAndNames)
            .isEqualTo(mapOf("/cache/video-editor-export.mp4" to "video (1).mp4"))
        assertThat(upload.specificStartMessage).isEqualTo(UPLOAD_MESSAGE)
    }

    @Test
    fun `test that onExportSucceeded queues a failure message when the output path is missing`(
        @TempDir tempDir: File,
    ) = runTest {
        initReadyViewModel(tempDir)
        advanceUntilIdle()

        underTest.onExportSucceeded(mock<Uri> { on { path } doReturn null })
        advanceUntilIdle()

        verifyBlocking(snackbarEventQueue) { queueMessage(eq("Couldn't save video")) }
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
