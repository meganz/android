package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class GetFileNameFromContentUriTest{

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getFileNameFromContentUri = GetFileNameFromContentUri(fileSystemRepository)

    // Test that the file name is returned when the uri is a content uri
    @Test
    fun `test that file name is returned when uri is a content uri`() = runTest {
        val uriOrPathString = "content://com.android.providers.downloads.documents/document/1234"
        val fileName = "file.txt"

        whenever(fileSystemRepository.isContentUri(uriOrPathString)).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriOrPathString)).thenReturn(fileName)

        assertEquals(fileName, getFileNameFromContentUri(uriOrPathString))
    }

    // Test that null is returned when the uri is not a content uri
    @Test
    fun `test that null is returned when uri is not a content uri`() = runTest {
        val uriOrPathString = "file:///storage/emulated/0/Download/file.txt"

        whenever(fileSystemRepository.isContentUri(uriOrPathString)).thenReturn(false)

        assertNull(getFileNameFromContentUri(uriOrPathString))
    }
}