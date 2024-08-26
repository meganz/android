package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class IsPathInsecureUseCaseTest {
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val underTest = IsPathInsecureUseCase(fileSystemRepository)

    @Test
    fun `test that invoke triggers isPathInsecure from fileSystemRepository`() = runTest {
        val path = "path"
        underTest(path)
        verify(fileSystemRepository).isPathInsecure(path)
    }

}