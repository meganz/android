package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorCameraUploadsSettingsActionsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorCameraUploadsSettingsActionsUseCaseTest {

    private lateinit var underTest: MonitorCameraUploadsSettingsActionsUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCameraUploadsSettingsActionsUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the camera uploads settings actions are being monitored`() = runTest {
        val cameraUploadsSettingsActionFlow = flowOf<CameraUploadsSettingsAction>()

        whenever(cameraUploadsRepository.monitorCameraUploadsSettingsActions()).thenReturn(
            cameraUploadsSettingsActionFlow
        )

        assertThat(underTest()).isEqualTo(cameraUploadsSettingsActionFlow)
    }
}