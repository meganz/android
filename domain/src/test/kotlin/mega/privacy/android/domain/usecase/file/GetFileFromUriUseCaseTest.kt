package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileFromUriUseCaseTest {
    private lateinit var underTest: GetFileFromUriUseCase

    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileFromUriUseCase(
            getCacheFileUseCase,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            getCacheFileUseCase,
            fileSystemRepository,
        )
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
    }

    @Test
    fun `test that file from uri is returned when uri represents a file`() = runTest {
        val uri = "file://file.txt"
        val file = mock<File>()
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uri, "x")).isEqualTo(file)
    }

    @Test
    fun `test that a copy of the file is returned when uri is a content uri`() = runTest {
        val uriString = "content://example.txt"
        val fileName = "example"
        val fileExtension = "txt"
        val file = mock<File>()
        val cacheFolderName = "chat"
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(fileName)
        whenever(fileSystemRepository.getFileExtensionFromUri(uriString)).thenReturn(fileExtension)
        whenever(getCacheFileUseCase(eq(cacheFolderName), any())).thenReturn(file)
        assertThat(underTest.invoke(uriString, cacheFolderName)).isEqualTo(file)
        verify(fileSystemRepository).copyContentUriToFile(uriString, file)
    }
}