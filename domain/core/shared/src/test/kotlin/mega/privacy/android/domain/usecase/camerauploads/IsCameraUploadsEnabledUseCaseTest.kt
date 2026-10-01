package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsCameraUploadsEnabledUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsCameraUploadsEnabledUseCaseTest {

    private lateinit var underTest: IsCameraUploadsEnabledUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    private var monitorCameraUploadsEnabled = MutableStateFlow<Boolean?>(false)

    @BeforeAll
    fun setUp() {
        whenever(
            cameraUploadsRepository.monitorCameraUploadsEnabled
        ) doReturn monitorCameraUploadsEnabled
        underTest = IsCameraUploadsEnabledUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that camera uploads is enabled`() = runTest {
        whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that camera uploads is disabled`() = runTest {
        whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(false)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that camera uploads is disabled if its status cannot be retrieved from the repository`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(null)

            assertThat(underTest()).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is returned`(isEnabled: Boolean) = runTest {
        monitorCameraUploadsEnabled.emit(isEnabled)

        underTest.monitorCameraUploadsEnabled.test {
            assertThat(expectMostRecentItem()).isEqualTo(isEnabled)
        }
    }

    @Test
    fun `test that false is returned as the default value when monitorCameraUploadsEnabled is null`() =
        runTest {
            monitorCameraUploadsEnabled.emit(null)

            underTest.monitorCameraUploadsEnabled.test {
                assertThat(expectMostRecentItem()).isFalse()
            }
        }
}
