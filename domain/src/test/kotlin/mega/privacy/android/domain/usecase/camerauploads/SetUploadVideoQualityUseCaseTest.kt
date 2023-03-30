package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetUploadVideoQualityUseCase]
 */
@ExperimentalCoroutinesApi
class SetUploadVideoQualityUseCaseTest {

    private lateinit var underTest: SetUploadVideoQualityUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = SetUploadVideoQualityUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that the videos uploaded through camera uploads will now retain their original resolutions`() =
        testSetUploadVideoQuality(VideoQuality.ORIGINAL)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in high quality`() =
        testSetUploadVideoQuality(VideoQuality.HIGH)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in medium quality`() =
        testSetUploadVideoQuality(VideoQuality.MEDIUM)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in low quality`() =
        testSetUploadVideoQuality(VideoQuality.LOW)

    private fun testSetUploadVideoQuality(input: VideoQuality) = runTest {
        underTest(input)

        verify(cameraUploadRepository).setUploadVideoQuality(input)
    }
}