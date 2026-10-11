package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilePrepareUseCaseTest {

    private lateinit var underTest: FilePrepareUseCase

    private val fileSystemRepository: FileSystemRepository = mock()
    private val cacheRepository: CacheRepository = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()

    @TempDir
    lateinit var tempDir: File

    private val cacheFolderName = "temp"
    private val contentUri = UriPath("content://provider/track.gpx")
    private val pipeDocument = DocumentEntity(
        name = "track.gpx",
        size = 0L,
        lastModified = 1000L,
        uri = contentUri,
    )

    @BeforeAll
    fun setUp() {
        underTest = FilePrepareUseCase(
            fileSystemRepository = fileSystemRepository,
            cacheRepository = cacheRepository,
            getCacheFileUseCase = getCacheFileUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository, cacheRepository, getCacheFileUseCase)
        whenever(cacheRepository.getCacheFolderNameForTransfer(false)).thenReturn(cacheFolderName)
    }

    @Test
    fun `test that invoke returns the document entities when they are regular files`() = runTest {
        val uris = listOf(contentUri)
        val documents = listOf(pipeDocument.copy(size = 100L))
        whenever(fileSystemRepository.getDocumentEntities(uris)).thenReturn(documents)
        whenever(fileSystemRepository.isRegularFile(contentUri)).thenReturn(true)

        assertThat(underTest(uris)).isEqualTo(documents)
        verify(fileSystemRepository, never()).copyContentUriToFile(any(), any())
    }

    @Test
    fun `test that invoke returns an empty list when uris are empty`() = runTest {
        whenever(fileSystemRepository.getDocumentEntities(emptyList())).thenReturn(emptyList())

        assertThat(underTest(emptyList())).isEmpty()
    }

    @Test
    fun `test that invoke copies the content to the cache when the uri is not a regular file`() =
        runTest {
            val uris = listOf(contentUri)
            val cacheFile = File(tempDir, pipeDocument.name)
            val content = "<gpx/>"
            whenever(fileSystemRepository.getDocumentEntities(uris)).thenReturn(listOf(pipeDocument))
            whenever(fileSystemRepository.isRegularFile(contentUri)).thenReturn(false)
            whenever(getCacheFileUseCase(cacheFolderName, pipeDocument.name)).thenReturn(cacheFile)
            whenever(fileSystemRepository.copyContentUriToFile(contentUri, cacheFile)).doAnswer {
                cacheFile.writeText(content)
            }

            assertThat(underTest(uris)).containsExactly(
                pipeDocument.copy(
                    uri = UriPath.fromFile(cacheFile),
                    size = content.length.toLong(),
                )
            )
        }

    @Test
    fun `test that invoke does not check folders`() = runTest {
        val uris = listOf(contentUri)
        val folder = pipeDocument.copy(isFolder = true)
        whenever(fileSystemRepository.getDocumentEntities(uris)).thenReturn(listOf(folder))

        assertThat(underTest(uris)).containsExactly(folder)
        verify(fileSystemRepository, never()).isRegularFile(any())
    }

    @Test
    fun `test that invoke keeps the original document when copying to the cache fails`() =
        runTest {
            val uris = listOf(contentUri)
            val cacheFile = File(tempDir, pipeDocument.name)
            whenever(fileSystemRepository.getDocumentEntities(uris)).thenReturn(listOf(pipeDocument))
            whenever(fileSystemRepository.isRegularFile(contentUri)).thenReturn(false)
            whenever(getCacheFileUseCase(cacheFolderName, pipeDocument.name)).thenReturn(cacheFile)
            whenever(fileSystemRepository.copyContentUriToFile(contentUri, cacheFile))
                .thenThrow(RuntimeException())

            assertThat(underTest(uris)).containsExactly(pipeDocument)
        }

    @Test
    fun `test that invoke keeps the original document when no cache file is available`() =
        runTest {
            val uris = listOf(contentUri)
            whenever(fileSystemRepository.getDocumentEntities(uris)).thenReturn(listOf(pipeDocument))
            whenever(fileSystemRepository.isRegularFile(contentUri)).thenReturn(false)
            whenever(getCacheFileUseCase(cacheFolderName, pipeDocument.name)).thenReturn(null)

            assertThat(underTest(uris)).containsExactly(pipeDocument)
            verify(fileSystemRepository, never()).copyContentUriToFile(any(), any())
        }
}
