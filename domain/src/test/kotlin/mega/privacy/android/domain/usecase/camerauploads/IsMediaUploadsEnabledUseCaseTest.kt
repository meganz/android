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
 * Test class for [IsMediaUploadsEnabledUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsMediaUploadsEnabledUseCaseTest {

    private lateinit var underTest: IsMediaUploadsEnabledUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsMediaUploadsEnabledUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun reset() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that media uploads is enabled`() = runTest {
        whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(true)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that media uploads is disabled`() = runTest {
        whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(false)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that the media uploads is disabled if its status cannot be retrieved from the repository`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(null)

            assertThat(underTest()).isFalse()
        }
}