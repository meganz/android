package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorCameraUploadsFolderDestinationUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorCameraUploadsFolderDestinationUseCaseTest {
    private lateinit var underTest: MonitorCameraUploadsFolderDestinationUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCameraUploadsFolderDestinationUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the camera uploads folder destination is being monitored`() = runTest {
        val cameraUploadsFolderDestinationUpdateFlow =
            flowOf<CameraUploadsFolderDestinationUpdate>()

        whenever(cameraUploadsRepository.monitorCameraUploadsFolderDestination()).thenReturn(
            cameraUploadsFolderDestinationUpdateFlow
        )

        assertThat(underTest()).isEqualTo(cameraUploadsFolderDestinationUpdateFlow)
    }
}