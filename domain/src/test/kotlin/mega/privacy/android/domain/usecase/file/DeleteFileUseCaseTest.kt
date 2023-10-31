package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteFileUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = mock()

    private val underTest = DeleteFileUseCase(fileSystemRepository)

    @Test
    fun `test that fileSystemRepository deleteFile is called when usecase is invoked`() = runTest {
        val filePath = "filePath"

        underTest(filePath)

        verify(fileSystemRepository).deleteFile(File(filePath))
    }
}