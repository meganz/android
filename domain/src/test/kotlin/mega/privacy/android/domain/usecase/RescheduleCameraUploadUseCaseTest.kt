package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.RescheduleCameraUploadUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class RescheduleCameraUploadUseCaseTest {
    private lateinit var underTest: RescheduleCameraUploadUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = RescheduleCameraUploadUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that if camera upload is rescheduled that repository method is invoked`() =
        runTest {
            underTest()
            verify(cameraUploadRepository).rescheduleCameraUpload()
        }
}
