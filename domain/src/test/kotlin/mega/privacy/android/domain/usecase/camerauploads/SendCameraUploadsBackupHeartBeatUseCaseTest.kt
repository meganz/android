package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
 * Test class for [SendCameraUploadsBackupHeartBeatUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendCameraUploadsBackupHeartBeatUseCaseTest {

    private lateinit var underTest: SendCameraUploadsBackupHeartBeatUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendCameraUploadsBackupHeartBeatUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadRepository)
    }

    @Test
    internal fun `test that camera uploads backup heart beat is sent when sync is enabled and backup id exists`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(69L)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadRepository, times(1)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    internal fun `test that camera uploads backup heart beat is not sent when sync is enabled and backup id does not exist`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(null)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    internal fun `test that camera uploads backup heart beat is not sent when sync is not enabled`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(69L)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }
}
