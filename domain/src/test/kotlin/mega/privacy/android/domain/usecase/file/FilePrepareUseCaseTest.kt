package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.toDocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilePrepareUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = mock()

    private lateinit var filePrepareUseCase: FilePrepareUseCase

    private val testScheduler = TestCoroutineScheduler()

    @BeforeAll
    fun setUp() {
        filePrepareUseCase = FilePrepareUseCase(
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun reset() {
        reset(fileSystemRepository)
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
        whenever(fileSystemRepository.getDocumentEntities(uriPaths)).thenReturn(documentEntities)

        val result = filePrepareUseCase(uriPaths)

        assertEquals(documentEntities, result)
    }

    @Test
    fun `test invoke with empty UriPaths`() = runTest(testScheduler) {
        val uriPaths = emptyList<UriPath>()

        whenever(fileSystemRepository.getDocumentEntities(uriPaths)).thenReturn(emptyList())
        val result = filePrepareUseCase(uriPaths)

        assertEquals(emptyList<DocumentEntity>(), result)
    }
}