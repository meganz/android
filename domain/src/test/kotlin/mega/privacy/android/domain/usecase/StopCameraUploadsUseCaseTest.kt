package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.never
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StopCameraUploadsUseCaseTest {
    private lateinit var underTest: StopCameraUploadsUseCase

    private val disableCameraUploadsUseCase = mock<DisableCameraUploadsUseCase>()
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StopCameraUploadsUseCase(
            disableCameraUploadsUseCase = disableCameraUploadsUseCase,
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            disableCameraUploadsUseCase,
            cameraUploadRepository,
        )
    }

    @Test
    fun `test that if Camera Uploads is not enabled then enable settings is not set to false`() =
        runTest {
            val shouldReschedule = true
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest(shouldReschedule = shouldReschedule)
            verify(disableCameraUploadsUseCase, never()).invoke()
        }

    @Test
    fun `test that if Camera Uploads is enabled and should not reschedule then Camera Uploads is disabled in settings`() =
        runTest {
            val shouldReschedule = false
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(shouldReschedule = shouldReschedule)
            verify(disableCameraUploadsUseCase).invoke()
        }

    @Test
    fun `test that if Camera Uploads is enabled and should reschedule then Camera Uploads is not disabled in settings`() =
        runTest {
            val shouldReschedule = true
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(shouldReschedule = shouldReschedule)
            verify(disableCameraUploadsUseCase, never()).invoke()
        }

    @Test
    fun `test that if camera upload is enabled that stop camera upload repository method is invoked`() =
        runTest {
            val shouldReschedule = true
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(shouldReschedule = shouldReschedule)
            verify(cameraUploadRepository).stopCameraUploads(shouldReschedule = shouldReschedule)
        }

    @Test
    fun `test that if camera upload is not enabled that stop camera upload repository method is not invoked`() =
        runTest {
            val shouldReschedule = true
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest(shouldReschedule = shouldReschedule)
            verify(cameraUploadRepository, never())
                .stopCameraUploads(shouldReschedule = shouldReschedule)
        }
}
