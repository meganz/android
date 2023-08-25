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
import org.mockito.kotlin.whenever

/**
 * Test class for [IsSecondaryFolderSetUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsSecondaryFolderSetUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderSetUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderSetUseCase(
            fileSystemRepository = fileSystemRepository,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            getSecondaryFolderPathUseCase,
        )
    }

    @ParameterizedTest(name = "folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the local secondary folder exists`(folderExists: Boolean) = runTest {
        val testPath = "test/path"

        whenever(fileSystemRepository.doesFolderExists(testPath)).thenReturn(folderExists)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(testPath)

        assertThat(underTest()).isEqualTo(folderExists)
    }
}
