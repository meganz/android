package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCameraUploadsStatusInfoUseCaseTest {

    private lateinit var underTest: MonitorCameraUploadsStatusInfoUseCase

    private val cameraUploadsRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCameraUploadsStatusInfoUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test the flow emits if camera uploads is enabled`() = runTest {
        whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)

        val event = CameraUploadsStatusInfo.Started

        whenever(cameraUploadsRepository.monitorCameraUploadsStatusInfo()).thenReturn(flowOf(event))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(event)
            awaitComplete()
        }
    }

    @Test
    fun `test the flow does not emit if camera uploads is disabled`() = runTest {
        whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(false)

        whenever(cameraUploadsRepository.monitorCameraUploadsStatusInfo()).thenReturn(mock())

        underTest().test {
            awaitComplete()
        }
    }
}
