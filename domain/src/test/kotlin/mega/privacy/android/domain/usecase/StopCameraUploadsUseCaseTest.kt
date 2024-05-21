package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StopCameraUploadsUseCaseTest {
    private lateinit var underTest: StopCameraUploadsUseCase

    private val disableCameraUploadsUseCase = mock<DisableCameraUploadsUseCase>()
    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StopCameraUploadsUseCase(
            disableCameraUploadsUseCase = disableCameraUploadsUseCase,
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            disableCameraUploadsUseCase,
            cameraUploadsRepository,
        )
    }

    @Test
    fun `test that Camera Uploads is not disabled if the Camera Uploads were previously disabled`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest(mock())
            verify(cameraUploadsRepository, never()).stopCameraUploads()
        }

    @Test
    fun `test that Camera Uploads is disabled if the Camera Uploads were previously enabled`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(mock())
            verify(cameraUploadsRepository).stopCameraUploads()
        }

    @Test
    fun `test that Camera Uploads should restart immediately if restart mode is RestartImmediately `() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(CameraUploadsRestartMode.RestartImmediately)
            verify(cameraUploadsRepository).startCameraUploads()
        }

    @Test
    fun `test that Camera Uploads should be rescheduled immediately if restart mode is Reschedule`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(CameraUploadsRestartMode.Reschedule)
            verify(cameraUploadsRepository).scheduleCameraUploads()
        }

    @Test
    fun `test that Camera Uploads should be stopped if restart mode is Stop`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(CameraUploadsRestartMode.Stop)
            verify(cameraUploadsRepository).stopCameraUploads()
        }

    @Test
    fun `test that Camera Uploads should be disabled if restart mode is StopAndDisable`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(CameraUploadsRestartMode.StopAndDisable)
            verify(cameraUploadsRepository).stopCameraUploadsAndBackupHeartbeat()
            verify(disableCameraUploadsUseCase).invoke()
        }
}
