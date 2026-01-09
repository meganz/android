package mega.privacy.android.feature.photos.presentation.cuprogress

import androidx.core.net.toUri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorCameraUploadsInProgressTransfersUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.math.BigInteger

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsProgressViewModelTest {

    private lateinit var underTest: CameraUploadsProgressViewModel

    private val monitorCameraUploadsInProgressTransfersUseCase: MonitorCameraUploadsInProgressTransfersUseCase =
        mock()
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase =
        mock()
    private val getThumbnailUseCase: GetThumbnailUseCase = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    private var cuInProgressTransferFlow =
        MutableStateFlow<Map<Long, InProgressTransfer>>(emptyMap())
    private var cuStatusInfoFlow =
        MutableStateFlow<CameraUploadsStatusInfo>(CameraUploadsStatusInfo.Unknown)

    @BeforeEach
    fun setup() {
        whenever(monitorCameraUploadsInProgressTransfersUseCase()) doReturn cuInProgressTransferFlow
        whenever(monitorCameraUploadsStatusInfoUseCase()) doReturn cuStatusInfoFlow
        underTest = CameraUploadsProgressViewModel(
            monitorCameraUploadsInProgressTransfersUseCase = monitorCameraUploadsInProgressTransfersUseCase,
            monitorCameraUploadsStatusInfoUseCase = monitorCameraUploadsStatusInfoUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            fileTypeIconMapper = fileTypeIconMapper
        )
    }

    @AfterEach
    fun tearDown() {
        cuInProgressTransferFlow = MutableStateFlow(emptyMap())
        cuStatusInfoFlow = MutableStateFlow(CameraUploadsStatusInfo.Unknown)
        reset(
            monitorCameraUploadsInProgressTransfersUseCase,
            monitorCameraUploadsStatusInfoUseCase,
            getThumbnailUseCase,
            fileTypeIconMapper
        )
    }

    @Test
    fun `test that the initial uiState is default`() = runTest {
        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.isLoading).isFalse()
            assertThat(item.transfers).isEmpty()
            assertThat(item.pendingCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that the loading is true when the cu status info is CheckFilesForUpload with no transfers`() =
        runTest {
            cuStatusInfoFlow.value = CameraUploadsStatusInfo.CheckFilesForUpload

            underTest.uiState.test {
                assertThat(expectMostRecentItem().isLoading).isTrue()
            }
        }

    @Test
    fun `test that the pending transfer count is successfully set when the cu status info is UploadProgress`() =
        runTest {
            cuStatusInfoFlow.value = CameraUploadsStatusInfo.UploadProgress(
                totalToUpload = 10,
                totalUploaded = 3,
                totalUploadedBytes = 1L,
                progress = Progress(1f),
                totalUploadBytes = 1L,
                areUploadsPaused = false,
            )

            underTest.uiState.test {
                assertThat(expectMostRecentItem().pendingCount).isEqualTo(7)
            }
        }

    @Test
    fun `test that transfers are successfully grouped into in-progress and queued`() = runTest {
        val inProgress = mock<InProgressTransfer.Upload> {
            on { priority } doReturn BigInteger.ONE
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        val queued = mock<InProgressTransfer.Upload> {
            on { priority } doReturn BigInteger.TWO
            on { state } doReturn TransferState.STATE_QUEUED
        }

        cuInProgressTransferFlow.value = mapOf(
            1L to inProgress,
            2L to queued
        )

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.transfers.size).isEqualTo(2)
            assertThat(item.transfers[0]).isEqualTo(
                CameraUploadsTransferType.InProgress(
                    items = listOf(inProgress)
                )
            )
            assertThat(item.transfers[1]).isEqualTo(
                CameraUploadsTransferType.InQueue(
                    items = listOf(queued)
                )
            )
        }
    }

    @Test
    fun `test that the local preview uri is successfully set when adding an upload transfer`() =
        runTest {
            val fileName = "image.jpg"
            val localPath = "/local/path/image.jpg"
            val transfer = InProgressTransfer.Upload(
                tag = 1,
                fileName = fileName,
                localPath = localPath,
                priority = BigInteger.ZERO,
                uniqueId = 1L,
                totalBytes = 10L * 1024 * 1024,
                isPaused = false,
                speed = 4L * 1024 * 1024,
                state = TransferState.STATE_ACTIVE,
                progress = Progress(0.6f),
            )
            whenever(fileTypeIconMapper(fileExtension = "jpg")) doReturn 123

            underTest.addTransfer(transfer)

            underTest.getTransferItemUiState(id = 1).test {
                val item = expectMostRecentItem()
                assertThat(item.fileTypeResId).isEqualTo(123)
                assertThat(item.previewUri).isEqualTo(localPath.toUri())
            }
        }

    @Test
    fun `test that the local preview uri is set to NULL when adding an upload transfer and the thumbnail file is null`() =
        runTest {
            val transfer = InProgressTransfer.Download(
                tag = 2,
                fileName = "image.png",
                nodeId = NodeId(123),
                priority = BigInteger.ZERO,
                uniqueId = 2L,
                totalBytes = 100,
                isPaused = false,
                speed = 100,
                state = TransferState.STATE_ACTIVE,
                progress = Progress(0.5F),
            )
            whenever(fileTypeIconMapper(fileExtension = "png")) doReturn 123
            whenever(getThumbnailUseCase(nodeId = 123, allowThrow = true)) doReturn null

            underTest.addTransfer(transfer)

            underTest.getTransferItemUiState(id = 2).test {
                assertThat(expectMostRecentItem().previewUri).isNull()
            }
        }

    @Test
    fun `test that the thumbnail is successfully set when adding a download transfer`() = runTest {
        val transfer = InProgressTransfer.Download(
            tag = 2,
            fileName = "image.png",
            nodeId = NodeId(123),
            priority = BigInteger.ZERO,
            uniqueId = 2L,
            totalBytes = 100,
            isPaused = false,
            speed = 100,
            state = TransferState.STATE_ACTIVE,
            progress = Progress(0.5F),
        )
        whenever(fileTypeIconMapper(fileExtension = "png")) doReturn 123
        val file = mock<File>()
        whenever(getThumbnailUseCase(nodeId = 123, allowThrow = true)) doReturn file

        underTest.addTransfer(transfer)

        underTest.getTransferItemUiState(id = 2).test {
            assertThat(expectMostRecentItem().fileTypeResId).isEqualTo(123)
        }
    }

    @Test
    fun `test that the same transfer id returns the same StateFlow`() {
        val flow1 = underTest.getTransferItemUiState(id = 10)
        val flow2 = underTest.getTransferItemUiState(id = 10)

        assertThat(flow1).isEqualTo(flow2)
    }
}
