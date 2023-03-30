package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [GetUploadOptionUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUploadOptionUseCaseTest {

    private lateinit var underTest: GetUploadOptionUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetUploadOptionUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that get upload option is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository).getUploadOption()
    }
}