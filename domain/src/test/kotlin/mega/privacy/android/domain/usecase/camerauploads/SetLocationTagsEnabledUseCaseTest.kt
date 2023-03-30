package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetLocationTagsEnabledUseCase]
 */
@ExperimentalCoroutinesApi
class SetLocationTagsEnabledUseCaseTest {

    private lateinit var underTest: SetLocationTagsEnabledUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = SetLocationTagsEnabledUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that set location tags is enabled`() = runTest {
        underTest(true)

        verify(cameraUploadRepository).setLocationTagsEnabled(true)
    }

    @Test
    fun `test that set location tags is disabled`() = runTest {
        underTest(false)

        verify(cameraUploadRepository).setLocationTagsEnabled(false)
    }
}