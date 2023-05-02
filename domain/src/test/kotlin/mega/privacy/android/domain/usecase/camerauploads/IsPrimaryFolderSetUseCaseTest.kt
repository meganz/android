package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [IsPrimaryFolderSetUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsPrimaryFolderSetUseCaseTest {

    private lateinit var underTest: IsPrimaryFolderSetUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsPrimaryFolderSetUseCase(
            fileSystemRepository = fileSystemRepository,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getPrimaryFolderPathUseCase)
    }

    @ParameterizedTest(name = "path: {0}")
    @ValueSource(strings = ["", " "])
    fun `test that the primary folder is not set when the path is empty`(primaryFolderPath: String) =
        runTest {
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            assertThat(underTest()).isFalse()
            verifyNoInteractions(fileSystemRepository)
        }

    @ParameterizedTest(name = "file exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the primary folder is set or not`(primaryFolderExists: Boolean) = runTest {
        val testPath = "test/primary/folder/path"

        whenever(getPrimaryFolderPathUseCase()).thenReturn(testPath)
        whenever(fileSystemRepository.doesFolderExists(testPath)).thenReturn(primaryFolderExists)

        assertThat(underTest()).isEqualTo(primaryFolderExists)
    }
}