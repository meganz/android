package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class StopCameraUploadUseCaseTest {
    private lateinit var underTest: StopCameraUploadUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = StopCameraUploadUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that if camera upload is enabled that stop camera upload repository method is invoked`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository).fireStopCameraUploadJob()
        }

    @Test
    fun `test that if camera upload is not enabled that stop camera upload repository method is not invoked`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository, times(0)).fireStopCameraUploadJob()
        }
}
