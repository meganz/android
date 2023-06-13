package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class StartCameraUploadUseCaseTest {
    private lateinit var underTest: StartCameraUploadUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = StartCameraUploadUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that if camera upload is enabled that start camera upload repository method is invoked`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository).fireCameraUploadJob()
        }

    @Test
    fun `test that if camera upload is not enabled that start camera upload repository method is not invoked`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository, times(0)).fireCameraUploadJob()
        }
}
