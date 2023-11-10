package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetPrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetPrimaryFolderPathUseCaseTest {
    private lateinit var underTest: SetPrimaryFolderPathUseCase

    private val setPrimaryFolderLocalPathUseCase = mock<SetPrimaryFolderLocalPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetPrimaryFolderPathUseCase(
            setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(setPrimaryFolderLocalPathUseCase)
    }

    @ParameterizedTest(name = "is in SD Card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the new primary folder path is set`(isInSDCard: Boolean) = runTest {
        val testPath = "test/new/primary/folder/path"
        underTest(newFolderPath = testPath)
        verify(setPrimaryFolderLocalPathUseCase).invoke(testPath)
    }
}
