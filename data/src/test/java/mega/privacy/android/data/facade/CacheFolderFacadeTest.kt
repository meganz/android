package mega.privacy.android.data.facade

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant.CHAT_TEMPORARY_FOLDER
import mega.privacy.android.data.gateway.FileGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheFolderFacadeTest {
    private lateinit var underTest: CacheFolderFacade

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val coroutineScope: CoroutineScope = TestScope(testDispatcher)

    private val context = mock<Context>()
    private val fileGateway = mock<FileGateway>()


    @BeforeAll
    fun setUp() {
        underTest = CacheFolderFacade(
            context,
            fileGateway,
            coroutineScope,
            testDispatcher,
        )
    }

    @BeforeEach
    fun reset() {
        reset(context, fileGateway)
    }

    @ParameterizedTest
    @MethodSource("provideFilesInCache")
    fun `test that isFileInCacheDirectory check the correct cache folders`(
        file: File,
    ) = runTest {
        stubCacheDirs()

        val actual = underTest.isFileInCacheDirectory(file)

        assertThat(actual).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideCacheFolders")
    fun `test that isFileInCacheDirectory return false for the cache folder itself`(
        file: File,
    ) = runTest {
        stubCacheDirs()

        val actual = underTest.isFileInCacheDirectory(file)

        assertThat(actual).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideFilesNotInCache")
    fun `test that isFileInCacheDirectory return false for files not in cache`(
        file: File,
    ) = runTest {
        stubCacheDirs()

        val actual = underTest.isFileInCacheDirectory(file)

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that getCacheSize returns the cache bytes reported by StorageStatsManager`() =
        runTest {
            val expected = 28_720_000L
            val storageStats = mock<StorageStats> {
                on { cacheBytes } doReturn expected
            }
            val storageStatsManager = mock<StorageStatsManager> {
                on { queryStatsForUid(anyOrNull(), any()) } doReturn storageStats
            }
            whenever(context.getSystemService(StorageStatsManager::class.java))
                .thenReturn(storageStatsManager)

            mockStatic(Process::class.java).use { processMock ->
                whenever(Process.myUid()).thenReturn(10123)

                val actual = underTest.getCacheSize()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getCacheSize falls back to summing cache folders when StorageStatsManager fails`() =
        runTest {
            whenever(context.getSystemService(StorageStatsManager::class.java))
                .thenThrow(RuntimeException("unavailable"))
            whenever(context.cacheDir) doReturn cacheDir
            whenever(context.externalCacheDir) doReturn externalCacheDir
            whenever(fileGateway.getTotalSize(cacheDir)) doReturn 5_000L
            whenever(fileGateway.getTotalSize(externalCacheDir)) doReturn 3_900L

            val actual = underTest.getCacheSize()

            assertThat(actual).isEqualTo(8_900L)
        }

    @Test
    fun `test that getPreviewFile returns the cached file when size and modification time match the node`(
        @TempDir tempDir: File,
    ) = runTest {
        whenever(context.externalCacheDir) doReturn tempDir
        val cached = createCachedFile(tempDir, PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME)

        val actual = underTest.getPreviewFile(PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME)

        assertThat(actual).isEqualTo(cached)
    }

    @Test
    fun `test that getPreviewFile returns null when the cached file size differs from the node`(
        @TempDir tempDir: File,
    ) = runTest {
        whenever(context.externalCacheDir) doReturn tempDir
        createCachedFile(tempDir, PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME)

        val actual =
            underTest.getPreviewFile(PREVIEW_FILE_NAME, NODE_SIZE + 1, NODE_MODIFICATION_TIME)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that getPreviewFile returns null when the cached file modification time differs from the node`(
        @TempDir tempDir: File,
    ) = runTest {
        whenever(context.externalCacheDir) doReturn tempDir
        createCachedFile(tempDir, PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME)

        val actual =
            underTest.getPreviewFile(PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME + 1)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that getPreviewFile returns null when no cached file exists`(
        @TempDir tempDir: File,
    ) = runTest {
        whenever(context.externalCacheDir) doReturn tempDir

        val actual = underTest.getPreviewFile(PREVIEW_FILE_NAME, NODE_SIZE, NODE_MODIFICATION_TIME)

        assertThat(actual).isNull()
    }

    private fun createCachedFile(
        dir: File,
        name: String,
        size: Long,
        modificationTimeSeconds: Long,
    ) = File(dir, name).apply {
        writeBytes(ByteArray(size.toInt()))
        setLastModified(modificationTimeSeconds * 1000L)
    }

    private fun stubCacheDirs() {
        whenever(context.filesDir) doReturn filesDir
        whenever(context.cacheDir) doReturn cacheDir
        whenever(context.externalCacheDir) doReturn externalCacheDir
        whenever(context.externalCacheDirs) doReturn externalCacheDirs.toTypedArray()
    }

    private fun provideCacheFolders() =
        externalCacheDirs
            .plus(cacheDir)
            .plus(externalCacheDir)
            .plus(File(filesDir, CHAT_TEMPORARY_FOLDER))

    private fun provideFilesInCache() =
        provideCacheFolders()
            .map { File(it, "hello.txt") }

    private fun provideFilesNotInCache() = listOf(
        File("/notInCache/", "hello.txt"),
        filesDir,
        File(filesDir, "hello.txt"),
        File(filesDir, CHAT_TEMPORARY_FOLDER + "2"),
        File(File(filesDir, CHAT_TEMPORARY_FOLDER + "2"), "hello.txt"),
    )
}

private const val PREVIEW_FILE_NAME = "document.docx"
private const val NODE_SIZE = 2048L
private const val NODE_MODIFICATION_TIME = 1_700_000_000L
private val filesDir = File("/data/data/com.example.megaApp/files/")
private val cacheDir = File("/data/data/com.example.megaApp/cache/", "")
private val externalCacheDir =
    File("/storage/emulated/0/Android/data/com.example.megaApp/cache/", "")
private val externalCacheDirs = (1..3).map {
    File("/storage/emulated/$it/Android/data/com.example.megaApp/cache/", "")
}