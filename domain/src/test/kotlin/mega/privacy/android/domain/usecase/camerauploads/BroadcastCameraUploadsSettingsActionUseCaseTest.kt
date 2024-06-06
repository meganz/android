package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [BroadcastCameraUploadsSettingsActionUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BroadcastCameraUploadsSettingsActionUseCaseTest {
    private lateinit var underTest: BroadcastCameraUploadsSettingsActionUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = BroadcastCameraUploadsSettingsActionUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that a broadcast to disable camera uploads is sent`() = runTest {
        val action = CameraUploadsSettingsAction.DisableCameraUploads

        underTest(action)

        verify(cameraUploadsRepository).broadCastCameraUploadSettingsActions(action)
    }

    @Test
    fun `test that a broadcast to disable media uploads is sent`() = runTest {
        val action = CameraUploadsSettingsAction.DisableMediaUploads

        underTest(action)

        verify(cameraUploadsRepository).broadCastCameraUploadSettingsActions(action)
    }
}