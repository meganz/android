package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [SetUploadFileNamesKeptUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetUploadFileNamesKeptUseCaseTest {

    private lateinit var underTest: SetUploadFileNamesKeptUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetUploadFileNamesKeptUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "keep file names: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the file names for uploads are handled`(keepFileNames: Boolean) = runTest {
        underTest(keepFileNames)

        verify(cameraUploadRepository, times(1)).setUploadFileNamesKept(keepFileNames)
    }
}