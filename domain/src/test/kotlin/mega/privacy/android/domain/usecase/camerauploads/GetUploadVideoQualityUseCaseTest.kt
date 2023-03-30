package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [GetUploadVideoQualityUseCase]
 */
@ExperimentalCoroutinesApi
class GetUploadVideoQualityUseCaseTest {

    private lateinit var underTest: GetUploadVideoQualityUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = GetUploadVideoQualityUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that get upload video quality is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository).getUploadVideoQuality()
    }
}