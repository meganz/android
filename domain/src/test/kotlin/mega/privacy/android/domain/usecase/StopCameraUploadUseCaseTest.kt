package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.never
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StopCameraUploadUseCaseTest {
    private lateinit var underTest: StopCameraUploadUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StopCameraUploadUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that if Camera Uploads is not enabled then enable settings is not set to false`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository, never()).setCameraUploadsEnabled(false)
        }

    @Test
    fun `test that if Camera Uploads is enabled then enable settings is set to false`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository).setCameraUploadsEnabled(false)
        }

    @Test
    fun `test that if camera upload is enabled that stop camera upload repository method is invoked`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository).fireStopCameraUploadJob()
        }

    @Test
    fun `test that if camera upload is not enabled that stop camera upload repository method is not invoked`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository, times(0)).fireStopCameraUploadJob()
        }
}
