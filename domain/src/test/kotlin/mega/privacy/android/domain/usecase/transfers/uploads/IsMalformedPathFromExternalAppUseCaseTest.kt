package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class IsMalformedPathFromExternalAppUseCaseTest {

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val underTest = IsMalformedPathFromExternalAppUseCase(fileSystemRepository)

    @Test
    fun `test that invoke triggers isMalformedPathFromExternalApp from fileSystemRepository`() =
        runTest {
            val action = "action"
            val path = "path"
            underTest(action, path)
            verify(fileSystemRepository).isMalformedPathFromExternalApp(action, path)
        }
}