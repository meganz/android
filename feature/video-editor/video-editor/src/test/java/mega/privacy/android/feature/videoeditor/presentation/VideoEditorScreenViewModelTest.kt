package mega.privacy.android.feature.videoeditor.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import android.content.Context
import android.net.Uri
import de.palm.composestateevents.StateEventWithContentTriggered
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
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
import org.mockito.kotlin.argumentCaptor
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
        private const val TRANSFER_TAG = 999
    }

    private lateinit var underTest: VideoEditorScreenViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getNodePreviewFileUseCase = mock<GetNodePreviewFileUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val downloadNodeUseCase = mock<DownloadNodeUseCase>()
    private val getNodeNameCollisionRenameNameUseCase = mock<GetNodeNameCollisionRenameNameUseCase>()
    private val getPreviewUseCase = mock<GetPreviewUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val context = mock<Context> {
        on { getString(sharedR.string.photo_editor_upload_message) } doReturn UPLOAD_MESSAGE
    }

    @BeforeEach
    fun setUp() {
        reset(
            getNodeByIdUseCase,
            getNodePreviewFileUseCase,
            getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase,
            getNodeNameCollisionRenameNameUseCase,
            getPreviewUseCase,
            getThumbnailUseCase,
            cancelTransferByTagUseCase,
            snackbarEventQueue,
        )
    }

    private fun initViewModel() {
        underTest = VideoEditorScreenViewModel(
            nodeHandle = NODE_HANDLE,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase = downloadNodeUseCase,
            getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase,
            getPreviewUseCase = getPreviewUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            applicationScope = applicationScope,
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
        // Blank, like an unset path: the ViewModel falls back to the recomputed destination.
        on { localPath } doReturn ""
    }

    private fun transferWith(floatValue: Float, tag: Int): Transfer = mock {
        on { progress } doReturn Progress(floatValue)
        on { this.tag } doReturn tag
        on { localPath } doReturn ""
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
    fun `test that file name and size are exposed once the node is resolved`(
        @TempDir tempDir: File,
    ) = runTest {
        File(tempDir, FILE_NAME).writeBytes(ByteArray(FILE_SIZE.toInt()))
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.fileName.isEmpty() && !state.isError) {
                state = awaitItem()
            }
            assertThat(state.fileName).isEqualTo(FILE_NAME)
            assertThat(state.fileSizeBytes).isEqualTo(FILE_SIZE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the preview image is used for the preview when available`(
        @TempDir tempDir: File,
    ) = runTest {
        File(tempDir, FILE_NAME).writeBytes(ByteArray(FILE_SIZE.toInt()))
        val preview = File(tempDir, "preview.jpg").apply { writeBytes(ByteArray(4)) }
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(getPreviewUseCase(node)) doReturn preview
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.previewImagePath == null) {
                state = awaitItem()
            }
            assertThat(state.previewImagePath).isEqualTo(preview.path)
            cancelAndIgnoreRemainingEvents()
        }
        // The thumbnail is not requested when a preview is available.
        org.mockito.kotlin.verifyNoInteractions(getThumbnailUseCase)
    }

    @Test
    fun `test that the thumbnail is used for the preview when no preview is available`(
        @TempDir tempDir: File,
    ) = runTest {
        File(tempDir, FILE_NAME).writeBytes(ByteArray(FILE_SIZE.toInt()))
        val thumbnail = File(tempDir, "thumbnail.jpg").apply { writeBytes(ByteArray(4)) }
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(getPreviewUseCase(node)) doReturn null
        whenever(getThumbnailUseCase(NODE_HANDLE)) doReturn thumbnail
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.previewImagePath == null) {
                state = awaitItem()
            }
            assertThat(state.previewImagePath).isEqualTo(thumbnail.path)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that an existing complete local copy is reused without downloading`(
        @TempDir tempDir: File,
    ) = runTest {
        val existing = File(tempDir, "offline-$FILE_NAME")
            .apply { writeBytes(ByteArray(FILE_SIZE.toInt())) }
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getNodePreviewFileUseCase(node)) doReturn existing
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.videoFilePath == null && !state.isError) {
                state = awaitItem()
            }
            assertThat(state.videoFilePath).isEqualTo(existing.path)
            assertThat(state.downloadProgress).isEqualTo(100)
            assertThat(state.isError).isFalse()
            // A reused copy is ready instantly, so the "Preparing video" dialog must never show.
            assertThat(state.isDownloading).isFalse()
        }
        // A complete local copy must not be downloaded again.
        org.mockito.kotlin.verifyNoInteractions(downloadNodeUseCase)
        // The cache download path is not even resolved when a local copy is reused.
        org.mockito.kotlin.verifyNoInteractions(getFilePreviewDownloadPathUseCase)
    }

    @Test
    fun `test that an incomplete local copy is not reused and triggers download`(
        @TempDir tempDir: File,
    ) = runTest {
        // A partial/derived file whose size does not match the node must be rejected.
        val partial = File(tempDir, "partial-$FILE_NAME")
            .apply { writeBytes(ByteArray((FILE_SIZE - 1).toInt())) }
        val finishEvent = TransferEvent.TransferFinishEvent(transferWithProgress(1f), error = null)
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getNodePreviewFileUseCase(node)) doReturn partial
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flow {
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
            assertThat(state.isError).isFalse()
        }
        verifyBlocking(downloadNodeUseCase) { invoke(any(), any(), anyOrNull(), any()) }
    }

    @Test
    fun `test that state is error when the node name contains a path separator`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "../$FILE_NAME"
            on { size } doReturn FILE_SIZE
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
            assertThat(state.isError).isTrue()
        }
        // The unsafe name must never reach the cache lookup or the download.
        org.mockito.kotlin.verifyNoInteractions(getNodePreviewFileUseCase)
        org.mockito.kotlin.verifyNoInteractions(downloadNodeUseCase)
    }

    @Test
    fun `test that the transfer's local path is used when the download finishes under another name`(
        @TempDir tempDir: File,
    ) = runTest {
        // The SDK escapes fs-incompatible characters, so the file can land under
        // a different name than the recomputed destination.
        val escapedFile = File(tempDir, "escaped-$FILE_NAME")
        val transfer = mock<Transfer> {
            on { progress } doReturn Progress(1f)
            on { localPath } doReturn escapedFile.path
        }
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, error = null)
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flow {
            escapedFile.writeBytes(ByteArray(FILE_SIZE.toInt()))
            emit(finishEvent)
        }
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (state.videoFilePath == null && !state.isError) {
                state = awaitItem()
            }
            assertThat(state.videoFilePath).isEqualTo(escapedFile.path)
            assertThat(state.isError).isFalse()
        }
    }

    @Test
    fun `test that isDownloading is true once a network download is in progress`(
        @TempDir tempDir: File,
    ) = runTest {
        val updateEvent = TransferEvent.TransferUpdateEvent(transferWithProgress(0.5f))
        val node = stubNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        // Flow emits progress but never finishes, so the download stays in progress.
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flowOf(updateEvent)
        initViewModel()

        underTest.uiState.test {
            var state = awaitItem()
            while (!state.isDownloading && !state.isError) {
                state = awaitItem()
            }
            assertThat(state.isDownloading).isTrue()
            assertThat(state.videoFilePath).isNull()
            cancelAndIgnoreRemainingEvents()
        }
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
    fun `test that onExportSucceeded uploads a non-mp4 source's copy under an mp4 name`(
        @TempDir tempDir: File,
    ) = runTest {
        // The encoded copy is always MP4, so the upload name must not keep the
        // source's container extension.
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "video.mkv"
            on { size } doReturn FILE_SIZE
            on { parentId } doReturn NodeId(PARENT_HANDLE)
            on { modificationTime } doReturn MODIFICATION_TIME
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        initViewModel()
        advanceUntilIdle()
        wheneverBlocking { getNodeNameCollisionRenameNameUseCase(any()) } doReturn "video (1).mp4"
        val outputUri = mock<Uri> { on { path } doReturn "/cache/video-editor-export.mp4" }

        underTest.onExportSucceeded(outputUri)
        advanceUntilIdle()

        val collisionCaptor = argumentCaptor<FileNameCollision>()
        verifyBlocking(getNodeNameCollisionRenameNameUseCase) { invoke(collisionCaptor.capture()) }
        assertThat(collisionCaptor.firstValue.name).isEqualTo("video.mp4")
        // The same derived name is exposed for the export progress dialog.
        assertThat(underTest.uiState.value.exportFileName).isEqualTo("video.mp4")
        val event = underTest.uiState.value.transferEvent
        val upload = (event as StateEventWithContentTriggered).content
                as TransferTriggerEvent.StartUpload.Files
        assertThat(upload.pathsAndNames)
            .isEqualTo(mapOf("/cache/video-editor-export.mp4" to "video (1).mp4"))
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
    fun `test that cancelDownload cancels the in-progress transfer by its tag`(
        @TempDir tempDir: File,
    ) = runTest {
        // Built outside the whenever stubbing to avoid nested stubbing.
        val node = stubNode()
        val updateEvent = TransferEvent.TransferUpdateEvent(transferWith(0.5f, TRANSFER_TAG))
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn node
        whenever(getFilePreviewDownloadPathUseCase()) doReturn tempDir.path
        // Emit a progress event so the transfer tag is captured, but never finish the download.
        whenever(downloadNodeUseCase(any(), any(), anyOrNull(), any())) doReturn flowOf(updateEvent)
        initViewModel()
        advanceUntilIdle()

        underTest.cancelDownload()
        advanceUntilIdle()

        verifyBlocking(cancelTransferByTagUseCase) { invoke(TRANSFER_TAG) }
    }

    @Test
    fun `test that cancelDownload does nothing when no transfer is in progress`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))) doReturn null
        initViewModel()
        advanceUntilIdle()

        underTest.cancelDownload()
        advanceUntilIdle()

        org.mockito.kotlin.verifyNoInteractions(cancelTransferByTagUseCase)
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
