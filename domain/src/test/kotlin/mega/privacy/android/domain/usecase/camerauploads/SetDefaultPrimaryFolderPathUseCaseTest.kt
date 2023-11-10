package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetDefaultPrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetDefaultPrimaryFolderPathUseCaseTest {

    private lateinit var underTest: SetDefaultPrimaryFolderPathUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val setPrimaryFolderLocalPathUseCase = mock<SetPrimaryFolderLocalPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetDefaultPrimaryFolderPathUseCase(
            fileSystemRepository = fileSystemRepository,
            setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            setPrimaryFolderLocalPathUseCase,
        )
    }

    @Test
    fun `test that the default primary folder path is set`() = runTest {
        val testDefaultPath = "test/default/path"
        fileSystemRepository.stub {
            onBlocking { doesExternalStorageDirectoryExists() }.thenReturn(true)
            onBlocking { localDCIMFolderPath }.thenReturn(testDefaultPath)
        }
        underTest()

        verify(setPrimaryFolderLocalPathUseCase).invoke(testDefaultPath)
    }

    @Test
    fun `test that no default primary folder path is set if the external storage directory does not exist`() =
        runTest {
            whenever(fileSystemRepository.doesExternalStorageDirectoryExists()).thenReturn(false)
            underTest()
            verifyNoInteractions(setPrimaryFolderLocalPathUseCase)
        }
}
