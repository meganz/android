package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [GetVideoCompressionSizeLimitUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoCompressionSizeLimitUseCaseTest {

    private lateinit var underTest: GetVideoCompressionSizeLimitUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoCompressionSizeLimitUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that get video compression size limit is invoked`() = runTest {
        underTest()

        verify(cameraUploadsRepository).getVideoCompressionSizeLimit()
    }
}