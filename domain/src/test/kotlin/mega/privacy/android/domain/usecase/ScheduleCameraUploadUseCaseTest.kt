package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ScheduleCameraUploadUseCaseTest {
    private lateinit var underTest: ScheduleCameraUploadUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = ScheduleCameraUploadUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that if camera upload is enabled that schedule camera upload repository method is invoked`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            underTest()
            verify(cameraUploadRepository).scheduleCameraUploadJob()
        }

    @Test
    fun `test that if camera upload is not enabled that schedule camera upload repository method is not invoked`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(false)
            underTest()
            verify(cameraUploadRepository, times(0)).scheduleCameraUploadJob()
        }
}
