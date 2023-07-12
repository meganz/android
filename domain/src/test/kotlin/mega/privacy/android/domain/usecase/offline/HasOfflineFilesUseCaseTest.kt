package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class HasOfflineFilesUseCaseTest {
    private lateinit var underTest: HasOfflineFilesUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeEach
    internal fun setUp() {
        underTest = HasOfflineFilesUseCase(
            fileSystemRepository = fileSystemRepository
        )
    }

    @Test
    internal fun `test that false is returned if folder does not exist`() = runTest {
        fileSystemRepository.stub {
            onBlocking { getOfflinePath() }.thenReturn(File(temporaryFolder, "NonExistent").path)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    internal fun `test that false is returned if folder does not contain any files`() = runTest {
        fileSystemRepository.stub {
            onBlocking { getOfflinePath() }.thenReturn(temporaryFolder.path)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    internal fun `test that true is returned if the offline folder contains files`() = runTest{
        withContext(Dispatchers.IO) {
            File(temporaryFolder, "OfflineFile.txt").createNewFile()
        }
        fileSystemRepository.stub {
            onBlocking { getOfflinePath() }.thenReturn(temporaryFolder.path)
        }

        assertThat(underTest()).isTrue()
    }
}