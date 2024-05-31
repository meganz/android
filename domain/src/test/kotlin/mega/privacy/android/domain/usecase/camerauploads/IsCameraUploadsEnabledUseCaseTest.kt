package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    @BeforeAll
    fun setUp() {
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
}