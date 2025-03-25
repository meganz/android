package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflineDocumentProviderRootFolderUseCaseTest {

    private lateinit var underTest: GetOfflineDocumentProviderRootFolderUseCase
    private val fileSystemRepository: FileSystemRepository = mock()
    private val mockedRootFolder = mock<File>()

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineDocumentProviderRootFolderUseCase(
            fileSystemRepository
        )
    }

    @Test
    fun `test that invoke returns null when offline root folder does not exist`() = runBlocking {
        whenever(fileSystemRepository.getOfflineFilesRootFolder()).thenReturn(mockedRootFolder)
        whenever(mockedRootFolder.exists()).thenReturn(false)

        val result = underTest.invoke()
        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke returns offline root folder when it exists`(): Unit = runBlocking {
        whenever(fileSystemRepository.getOfflineFilesRootFolder()).thenReturn(mockedRootFolder)
        whenever(mockedRootFolder.exists()).thenReturn(true)

        val result = underTest.invoke()
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(mockedRootFolder)
    }
}