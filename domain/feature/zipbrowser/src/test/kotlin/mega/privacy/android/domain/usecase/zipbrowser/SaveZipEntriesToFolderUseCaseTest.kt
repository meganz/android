package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveEntry
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.entity.zipbrowser.ZipBatchExtractionState
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

/**
 * The archive is already a local file, so saving a selection is a local copy: the entries are
 * written out of the archive and then copied into the folder the user picked.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveZipEntriesToFolderUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val underTest = SaveZipEntriesToFolderUseCase(
        zipArchiveRepository = zipArchiveRepository,
        fileSystemRepository = fileSystemRepository,
    )

    @TempDir
    lateinit var tempDir: File

    private lateinit var staging: File

    @BeforeEach
    fun setUp() {
        reset(zipArchiveRepository, fileSystemRepository)
        staging = File(tempDir, "staging").apply { mkdirs() }
        zipArchiveRepository.stub {
            onBlocking { newStagingFolderPath() } doAnswer { staging.absolutePath }
            onBlocking { getExtractionFreeBytes() } doAnswer { Long.MAX_VALUE }
        }
        fileSystemRepository.stub {
            onBlocking { isContentUri(any()) } doAnswer { false }
            onBlocking { isFileUri(any()) } doAnswer { false }
        }
    }

    @Test
    fun `test that a selected folder is copied with the structure it had in the archive`() =
        runTest {
            stubIndex(
                entry(0, "docs", isDirectory = true),
                entry(1, "docs/deep", isDirectory = true),
                entry(2, "docs/deep/a.txt"),
            )
            stubExtraction()
            val destination = stubFolderDestination()

            underTest(ARCHIVE, listOf("docs"), UriPath(destination.absolutePath)).toList()

            // The source handed to the copy is the folder, so its children travel with it.
            verify(fileSystemRepository).copyFiles(File(staging, "docs"), destination)
        }

    @Test
    fun `test that each selected entry is written at its own path inside the archive`() = runTest {
        stubIndex(
            entry(0, "docs", isDirectory = true),
            entry(1, "docs/deep", isDirectory = true),
            entry(2, "docs/deep/a.txt"),
        )
        stubExtraction()
        stubFolderDestination()

        underTest(ARCHIVE, listOf("docs"), UriPath(File(tempDir, "out").absolutePath)).toList()

        verify(zipArchiveRepository).extractEntryToFolder(
            eq(ARCHIVE),
            eq("docs/deep/a.txt"),
            eq(staging.absolutePath),
            anyOrNull(),
        )
    }

    @Test
    fun `test that a document tree destination is copied into rather than written to directly`() =
        runTest {
            stubIndex(entry(0, "a.txt"))
            stubExtraction()
            val destination = UriPath("content://tree/primary%3ADownload")
            fileSystemRepository.stub {
                onBlocking { isContentUri(destination.value) } doAnswer { true }
                onBlocking { copyFilesToDocumentUri(any(), any()) } doAnswer { 1 }
            }

            underTest(ARCHIVE, listOf("a.txt"), destination).toList()

            verify(fileSystemRepository).copyFilesToDocumentUri(File(staging, "a.txt"), destination)
            verify(fileSystemRepository, never()).copyFiles(any(), any())
        }

    @Test
    fun `test that the staging folder is removed once the selection has been saved`() = runTest {
        stubIndex(entry(0, "a.txt"))
        stubExtraction()
        stubFolderDestination()

        underTest(ARCHIVE, listOf("a.txt"), UriPath(File(tempDir, "out").absolutePath)).toList()

        verify(fileSystemRepository).deleteFolderAndItsFiles(staging.absolutePath)
    }

    @Test
    fun `test that nothing is written when the destination has no room for the selection`() =
        runTest {
            stubIndex(entry(0, "a.txt", size = 100L))
            zipArchiveRepository.stub { onBlocking { getExtractionFreeBytes() } doAnswer { 1L } }

            val states = underTest(ARCHIVE, listOf("a.txt"), UriPath("/out")).toList()

            assertThat(states.last()).isInstanceOf(ZipBatchExtractionState.Failed::class.java)
            verify(zipArchiveRepository, never())
                .extractEntryToFolder(any(), any(), any(), anyOrNull())
        }

    @Test
    fun `test that an entry that cannot be written does not lose the ones that could`() = runTest {
        stubIndex(entry(0, "good.txt"), entry(1, "bad.txt"))
        whenever(
            zipArchiveRepository.extractEntryToFolder(eq(ARCHIVE), eq("good.txt"), any(), anyOrNull())
        ).thenReturn(flowOf(ZipExtractionState.Completed(File(staging, "good.txt").absolutePath)))
        whenever(
            zipArchiveRepository.extractEntryToFolder(eq(ARCHIVE), eq("bad.txt"), any(), anyOrNull())
        ).thenReturn(
            flowOf(
                ZipExtractionState.Failed(ZipArchiveException.EntryExtractionFailed("bad.txt"))
            )
        )
        File(staging, "good.txt").writeText("ok")
        stubFolderDestination()

        val states = underTest(
            ARCHIVE,
            listOf("good.txt", "bad.txt"),
            UriPath(File(tempDir, "out").absolutePath),
        ).toList()

        val finished = states.last() as ZipBatchExtractionState.Finished
        assertThat(finished.extractedPaths).containsExactly("good.txt")
        assertThat(finished.failures.keys).containsExactly("bad.txt")
    }

    private fun stubExtraction() {
        whenever(zipArchiveRepository.extractEntryToFolder(any(), any(), any(), anyOrNull()))
            .doAnswer { invocation ->
                val entryPath = invocation.arguments[1] as String
                File(staging, entryPath).apply {
                    parentFile?.mkdirs()
                    writeText("extracted")
                }
                flowOf(ZipExtractionState.Completed(File(staging, entryPath).absolutePath))
            }
    }

    private suspend fun stubFolderDestination(): File {
        val destination = File(tempDir, "out").apply { mkdirs() }
        fileSystemRepository.stub {
            onBlocking { getFileByPath(any()) } doAnswer { destination }
            onBlocking { copyFiles(any(), any()) } doAnswer { 1 }
        }
        return destination
    }

    private fun stubIndex(vararg entries: ZipArchiveEntry) {
        val index = ZipArchiveIndex(ARCHIVE, entries.associateBy { it.path }, isEncrypted = false)
        zipArchiveRepository.stub { onBlocking { getArchiveIndex(ARCHIVE) } doAnswer { index } }
    }

    private fun entry(
        index: Int,
        path: String,
        isDirectory: Boolean = false,
        size: Long = 1L,
    ) = ZipArchiveEntry(
        index = index,
        path = path,
        name = path.substringAfterLast('/'),
        parentPath = path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() },
        size = size,
        modificationTime = 0L,
        isDirectory = isDirectory,
        isEncrypted = false,
        isNameDecodable = true,
    )

    private companion object {
        const val ARCHIVE = "/storage/a.zip"
    }
}
