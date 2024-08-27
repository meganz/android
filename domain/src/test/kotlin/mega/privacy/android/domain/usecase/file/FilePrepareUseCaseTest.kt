package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.toDocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilePrepareUseCaseTest {

    private val getFileForUploadUseCase: GetFileForUploadUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    private lateinit var filePrepareUseCase: FilePrepareUseCase

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeAll
    fun setUp() {
        filePrepareUseCase = FilePrepareUseCase(
            getFileForUploadUseCase,
            fileSystemRepository,
            testDispatcher
        )
    }

    @BeforeEach
    fun reset() {
        reset(getFileForUploadUseCase, fileSystemRepository)
    }

    @Test
    fun `test invoke with valid UriPaths`() = runTest(testScheduler) {
        val uriPaths = listOf(UriPath("uri1"), UriPath("uri2"))
        val file1 = mock<File> {
            on { name } doReturn "name1"
            on { length() } doReturn 100L
            on { lastModified() } doReturn 1000L
            on { absolutePath } doReturn "uri1"
        }
        val file2 = mock<File> {
            on { name } doReturn "name2"
            on { length() } doReturn 200L
            on { lastModified() } doReturn 2000L
            on { absolutePath } doReturn "uri2"
        }
        val documentEntities = listOf(file1.toDocumentEntity(), file2.toDocumentEntity())
        whenever(fileSystemRepository.getDocumentFileName(UriPath("uri1"))).thenReturn("name1")
        whenever(fileSystemRepository.getDocumentFileName(UriPath("uri2"))).thenReturn("name2")
        `when`(getFileForUploadUseCase("uri1", false)).thenReturn(file1)
        `when`(getFileForUploadUseCase("uri2", false)).thenReturn(file2)

        val result = filePrepareUseCase(uriPaths)

        assertEquals(documentEntities, result)
    }

    @Test
    fun `test invoke with empty UriPaths`() = runTest(testScheduler) {
        val uriPaths = emptyList<UriPath>()

        val result = filePrepareUseCase(uriPaths)

        assertEquals(emptyList<DocumentEntity>(), result)
    }

    @Test
    fun `test invoke with null DocumentEntities`() = runTest(testScheduler) {
        val uriPaths = listOf(UriPath("uri1"), UriPath("uri2"))
        whenever(fileSystemRepository.getDocumentFileName(UriPath("uri1"))).thenReturn("name1")
        whenever(fileSystemRepository.getDocumentFileName(UriPath("uri2"))).thenReturn("name2")
        `when`(getFileForUploadUseCase("uri1", false)).thenReturn(null)
        `when`(getFileForUploadUseCase("uri2", false)).thenReturn(null)

        val result = filePrepareUseCase(uriPaths)

        assertEquals(emptyList<DocumentEntity>(), result)
    }
}