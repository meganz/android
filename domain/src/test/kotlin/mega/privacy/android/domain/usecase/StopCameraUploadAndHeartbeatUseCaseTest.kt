package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class StopCameraUploadAndHeartbeatUseCaseTest {
    private lateinit var underTest: StopCameraUploadAndHeartbeatUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = StopCameraUploadAndHeartbeatUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that if camera upload and heartbeat is stopped that repository method is invoked`() =
        runTest {
            underTest()
            verify(cameraUploadRepository).stopCameraUploadSyncHeartbeatWorkers()
        }
}
