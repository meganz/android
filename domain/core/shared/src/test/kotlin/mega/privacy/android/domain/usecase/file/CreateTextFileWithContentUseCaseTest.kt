package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateTextFileWithContentUseCaseTest {

    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val cacheRepository = mock<CacheRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val underTest = CreateTextFileWithContentUseCase(
        getCacheFileUseCase = getCacheFileUseCase,
        cacheRepository = cacheRepository,
        fileSystemRepository = fileSystemRepository,
    )

    @AfterEach
    fun resetMocks() {
        reset(getCacheFileUseCase, cacheRepository, fileSystemRepository)
    }

    @Test
    fun `test that invoke writes content to cache file and returns its UriPath`() = runTest {
        val fileName = "shared.txt"
        val content = "hello world"
        val cacheFile = File("/tmp/cache/shared.txt")
        whenever(cacheRepository.getCacheFolderNameForTransfer(false))
            .thenReturn(CACHE_FOLDER)
        whenever(getCacheFileUseCase(CACHE_FOLDER, fileName)).thenReturn(cacheFile)

        val result = underTest(fileName, content)

        verify(fileSystemRepository).writeTextToPath(cacheFile.absolutePath, content)
        assertThat(result).isEqualTo(UriPath.fromFile(cacheFile))
    }

    @Test
    fun `test that invoke uses the transfer cache folder for non chat transfers`() = runTest {
        val cacheFile = File("/tmp/cache/note.txt")
        whenever(cacheRepository.getCacheFolderNameForTransfer(false))
            .thenReturn(CACHE_FOLDER)
        whenever(getCacheFileUseCase(CACHE_FOLDER, "note.txt")).thenReturn(cacheFile)

        underTest("note.txt", "content")

        verify(cacheRepository).getCacheFolderNameForTransfer(false)
        verify(getCacheFileUseCase).invoke(CACHE_FOLDER, "note.txt")
    }

    @Test
    fun `test that invoke returns null and does not write when cache file cannot be created`() =
        runTest {
            whenever(cacheRepository.getCacheFolderNameForTransfer(false))
                .thenReturn(CACHE_FOLDER)
            whenever(getCacheFileUseCase(CACHE_FOLDER, "missing.txt")).thenReturn(null)

            val result = underTest("missing.txt", "content")

            assertThat(result).isNull()
            verify(fileSystemRepository, never()).writeTextToPath(any(), any())
        }

    companion object {
        private const val CACHE_FOLDER = "share-cache"
    }
}
