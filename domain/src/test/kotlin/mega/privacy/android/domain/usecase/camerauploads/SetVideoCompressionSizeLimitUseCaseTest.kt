package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetVideoCompressionSizeLimitUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetVideoCompressionSizeLimitUseCaseTest {

    private lateinit var underTest: SetVideoCompressionSizeLimitUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetVideoCompressionSizeLimitUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that the new video compression size limit is set`() = runTest {
        underTest(300)

        verify(cameraUploadRepository).setVideoCompressionSizeLimit(300)
    }
}