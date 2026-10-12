package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetVideoCompressionSizeLimitUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetVideoCompressionSizeLimitUseCaseTest {

    private lateinit var underTest: SetVideoCompressionSizeLimitUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetVideoCompressionSizeLimitUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeAll
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the new video compression size limit is set`() = runTest {
        underTest(300)

        verify(cameraUploadsRepository).setVideoCompressionSizeLimit(300)
    }
}