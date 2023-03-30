package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetUploadOptionUseCase]
 */
@ExperimentalCoroutinesApi
class SetUploadOptionUseCaseTest {

    private lateinit var underTest: SetUploadOptionUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = SetUploadOptionUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that the new upload option is only photos`() =
        testSetUploadOption(UploadOption.PHOTOS)

    @Test
    fun `test that the new upload option is only videos`() =
        testSetUploadOption(UploadOption.VIDEOS)

    @Test
    fun `test that the new upload option is both photos and videos`() =
        testSetUploadOption(UploadOption.PHOTOS_AND_VIDEOS)


    private fun testSetUploadOption(input: UploadOption) = runTest {
        underTest(input)

        verify(cameraUploadRepository).setUploadOption(input)
    }
}