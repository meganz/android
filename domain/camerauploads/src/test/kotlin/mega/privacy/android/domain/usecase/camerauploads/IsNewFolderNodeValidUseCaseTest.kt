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
 * Test class for [IsNewFolderNodeValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsNewFolderNodeValidUseCaseTest {

    private lateinit var underTest: IsNewFolderNodeValidUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsNewFolderNodeValidUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the new folder node is invalid when it is null`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @Test
    fun `test that the new folder node is invalid when its handle is invalid`() = runTest {
        val invalidHandle = -1L
        whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)

        assertThat(underTest(invalidHandle)).isFalse()
    }

    @Test
    fun `test that the new folder node is valid`() = runTest {
        whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)

        assertThat(underTest(123456L)).isTrue()
    }
}