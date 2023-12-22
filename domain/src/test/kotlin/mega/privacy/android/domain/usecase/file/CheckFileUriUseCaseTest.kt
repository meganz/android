package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckFileUriUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = Mockito.mock()

    private val underTest = CheckFileUriUseCase(fileSystemRepository)

    @Test
    fun `test that fileSystemRepository checkFileExistsByUriPath is called when usecase is invoked`() =
        runTest {
            val uriPath = "uriPath"

            underTest(uriPath)

            Mockito.verify(fileSystemRepository).checkFileExistsByUriPath(uriPath)
        }
}