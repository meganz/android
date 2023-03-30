package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [AreLocationTagsEnabledUseCase]
 */
@ExperimentalCoroutinesApi
class AreLocationTagsEnabledUseCaseTest {

    private lateinit var underTest: AreLocationTagsEnabledUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = AreLocationTagsEnabledUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that are location tags enabled is invoked`() =
        runTest {
            underTest()

            verify(cameraUploadRepository).areLocationTagsEnabled()
        }
}