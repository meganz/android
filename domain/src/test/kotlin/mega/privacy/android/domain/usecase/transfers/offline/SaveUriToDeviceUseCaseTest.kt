package mega.privacy.android.domain.usecase.transfers.offline


import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveUriToDeviceUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = mock()
    private lateinit var saveUriToDeviceUseCase: SaveUriToDeviceUseCase

    @BeforeAll
    fun setUp() {
        saveUriToDeviceUseCase = SaveUriToDeviceUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that invoke correctly when destination is content uri`() = runTest {
        val name = "testName"
        val source = UriPath("sourceUri")
        val destination = UriPath("content://destinationUri")
        whenever(fileSystemRepository.isContentUri(destination.value)).thenReturn(true)
        whenever(fileSystemRepository.copyUri(name, source, destination)).thenReturn(Unit)

        saveUriToDeviceUseCase(name, source, destination)
        verify(fileSystemRepository).copyUri(name, source, destination)
    }

    @Test
    fun `test that invoke correctly when destination is file uri`() = runTest {
        val name = "testName"
        val source = UriPath("sourceUri")
        val destination = UriPath("file://destinationUri")

        whenever(fileSystemRepository.isContentUri(destination.value)).thenReturn(false)
        whenever(fileSystemRepository.isFileUri(destination.value)).thenReturn(true)
        val destinationFile = mock<File>()
        whenever(fileSystemRepository.getFileFromFileUri(destination.value)).thenReturn(
            destinationFile
        )
        whenever(fileSystemRepository.copyUri(name, source, destinationFile)).thenReturn(Unit)

        saveUriToDeviceUseCase(name, source, destination)

        verify(fileSystemRepository).copyUri(name, source, destinationFile)
    }

    @Test
    fun `test that invoke correctly when destination is file path`() = runTest {
        val name = "testName"
        val source = UriPath("sourceUri")
        val destination = UriPath("destinationPath")
        whenever(fileSystemRepository.isContentUri(destination.value)).thenReturn(false)
        whenever(fileSystemRepository.isFileUri(destination.value)).thenReturn(false)
        val file = mock<File>()
        whenever(fileSystemRepository.getFileByPath(destination.value)).thenReturn(file)

        saveUriToDeviceUseCase(name, source, destination)

        verify(fileSystemRepository).copyUri(name, source, file)
    }
}