package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [PreparePrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PreparePrimaryFolderPathUseCaseTest {

    private lateinit var underTest: PreparePrimaryFolderPathUseCase

    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val isPrimaryFolderPathValidUseCase = mock<IsPrimaryFolderPathValidUseCase>()
    private val setDefaultPrimaryFolderPathUseCase = mock<SetDefaultPrimaryFolderPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = PreparePrimaryFolderPathUseCase(
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
            setDefaultPrimaryFolderPathUseCase = setDefaultPrimaryFolderPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPrimaryFolderPathUseCase,
            isPrimaryFolderPathValidUseCase,
            setDefaultPrimaryFolderPathUseCase,
        )
    }

    @ParameterizedTest(name = "is path valid: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the primary folder path is prepared`(isPathValid: Boolean) = runTest {
        val testPath = "test/path"

        whenever(getPrimaryFolderPathUseCase()).thenReturn(testPath)
        whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(isPathValid)
        underTest()
        if (isPathValid) {
            verifyNoInteractions(setDefaultPrimaryFolderPathUseCase)
        } else {
            verify(setDefaultPrimaryFolderPathUseCase).invoke()
        }
    }
}