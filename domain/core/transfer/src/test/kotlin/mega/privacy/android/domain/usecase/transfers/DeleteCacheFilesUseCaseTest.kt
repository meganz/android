package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteCacheFilesUseCaseTest {
    private lateinit var underTest: DeleteCacheFilesUseCase

    private val cacheRepository = mock<CacheRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteCacheFilesUseCase(
            cacheRepository,
            fileSystemRepository,
        )
    }

    @BeforeEach()
    fun cleanUp() = reset(
        cacheRepository,
        fileSystemRepository,
    )

    @Test
    fun `test that file is deleted when it is in the cache folder`() = runTest {
        val path = UriPath("path/file.txt")
        val file = File(path.value)
        whenever(cacheRepository.isFileInCacheDirectory(file)) doReturn true

        underTest(listOf(path))

        verify(fileSystemRepository).deleteFile(file)
    }


    @Test
    fun `test that file is not deleted when it is not in the cache folder`() = runTest {
        val path = UriPath("path/file.txt")
        val file = File(path.value)
        whenever(cacheRepository.isFileInCacheDirectory(file)) doReturn false

        underTest(listOf(path))

        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that files are deleted when and only when are in the cache folder`() = runTest {
        val paths = (0..10).map { UriPath("path/file$it.txt") }
        val files = paths.map { File(it.value) }
        val filesInCache = files.filterIndexed { index, file ->
            index.rem(2) == 0
        }
        files.forEach { file ->
            whenever(cacheRepository.isFileInCacheDirectory(file)) doReturn (file in filesInCache)
        }

        underTest(paths)

        filesInCache.forEach {
            verify(fileSystemRepository).deleteFile(it)
        }
        verifyNoMoreInteractions(fileSystemRepository)
    }
}