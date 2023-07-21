package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [ReportUploadStatusUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReportUploadStatusUseCaseTest {

    private lateinit var underTest: ReportUploadStatusUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ReportUploadStatusUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that camera uploads status is sent when the backup id exists`() =
        runTest {
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(69L)
            underTest(
                cameraUploadFolderType = CameraUploadFolderType.Primary,
                heartbeatStatus = HeartbeatStatus.values().random(),
                pendingUploads = 69,
                lastNodeHandle = 1L,
                lastTimestamp = 60L,
            )
            verify(cameraUploadRepository).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    fun `test that media uploads status is sent when the backup id exists`() =
        runTest {
            whenever(cameraUploadRepository.getMuBackUpId()).thenReturn(69L)
            underTest(
                cameraUploadFolderType = CameraUploadFolderType.Secondary,
                heartbeatStatus = HeartbeatStatus.values().random(),
                pendingUploads = 69,
                lastNodeHandle = 1L,
                lastTimestamp = 60L,
            )
            verify(cameraUploadRepository).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    fun `test that camera uploads status is not sent when the backup id does not exist`() =
        runTest {
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(null)
            underTest(
                cameraUploadFolderType = CameraUploadFolderType.Primary,
                heartbeatStatus = HeartbeatStatus.values().random(),
                pendingUploads = 69,
                lastNodeHandle = 1L,
                lastTimestamp = 60L,
            )
            verify(cameraUploadRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    fun `test that media uploads status is not sent when the backup id does not exist`() =
        runTest {
            whenever(cameraUploadRepository.getMuBackUpId()).thenReturn(null)
            underTest(
                cameraUploadFolderType = CameraUploadFolderType.Secondary,
                heartbeatStatus = HeartbeatStatus.values().random(),
                pendingUploads = 69,
                lastNodeHandle = 1L,
                lastTimestamp = 60L,
            )
            verify(cameraUploadRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }
}
