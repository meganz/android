package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveEntry
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.entity.zipbrowser.ZipBatchExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractZipEntriesUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val extractZipEntryToFileUseCase = mock<ExtractZipEntryToFileUseCase>()
    private val underTest = ExtractZipEntriesUseCase(
        zipArchiveRepository = zipArchiveRepository,
        extractZipEntryToFileUseCase = extractZipEntryToFileUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        reset(zipArchiveRepository, extractZipEntryToFileUseCase)
    }

    @Test
    fun `test that invoke extracts every selected file`() = runTest {
        stub(entry(0, "a.txt"), entry(1, "b.txt"))

        val states = underTest(ARCHIVE, listOf("a.txt", "b.txt")).toList()

        verify(extractZipEntryToFileUseCase)(ARCHIVE, "a.txt", null)
        verify(extractZipEntryToFileUseCase)(ARCHIVE, "b.txt", null)
        assertThat((states.last() as ZipBatchExtractionState.Finished).extractedPaths).hasSize(2)
    }

    @Test
    fun `test that invoke expands a selected folder to the files below it`() = runTest {
        stub(
            entry(0, "docs", isDirectory = true),
            entry(1, "docs/a.txt"),
            entry(2, "docs/sub", isDirectory = true),
            entry(3, "docs/sub/b.txt"),
            entry(4, "other.txt"),
        )

        underTest(ARCHIVE, listOf("docs")).toList()

        verify(extractZipEntryToFileUseCase)(ARCHIVE, "docs/a.txt", null)
        verify(extractZipEntryToFileUseCase)(ARCHIVE, "docs/sub/b.txt", null)
        verify(extractZipEntryToFileUseCase, never())(ARCHIVE, "other.txt", null)
    }

    @Test
    fun `test that invoke extracts an entry once when it is selected twice`() = runTest {
        stub(entry(0, "docs", isDirectory = true), entry(1, "docs/a.txt"))

        underTest(ARCHIVE, listOf("docs", "docs/a.txt")).toList()

        verify(extractZipEntryToFileUseCase)(ARCHIVE, "docs/a.txt", null)
    }

    @Test
    fun `test that invoke reports progress against the number of files`() = runTest {
        stub(entry(0, "a.txt"), entry(1, "b.txt"))

        val progress = underTest(ARCHIVE, listOf("a.txt", "b.txt")).toList()
            .filterIsInstance<ZipBatchExtractionState.InProgress>()

        assertThat(progress.map { it.completed }).containsExactly(0, 1).inOrder()
        assertThat(progress.all { it.total == 2 }).isTrue()
    }

    @Test
    fun `test that invoke keeps going and summarises when one entry fails`() = runTest {
        stub(entry(0, "a.txt"), entry(1, "bad.txt"), entry(2, "c.txt"))
        whenever(extractZipEntryToFileUseCase(ARCHIVE, "bad.txt", null))
            .thenAnswer { throw ZipArchiveException.EntryExtractionFailed("bad.txt") }

        val finished = underTest(ARCHIVE, listOf("a.txt", "bad.txt", "c.txt")).toList()
            .last() as ZipBatchExtractionState.Finished

        assertThat(finished.extractedPaths).hasSize(2)
        assertThat(finished.failures.keys).containsExactly("bad.txt")
        assertThat(finished.hasFailures).isTrue()
    }

    @Test
    fun `test that invoke fails before writing anything when there is not enough room`() = runTest {
        stub(entry(0, "big.bin", size = 5_000L), freeBytes = 100L)

        val states = underTest(ARCHIVE, listOf("big.bin")).toList()

        val failure = states.single() as ZipBatchExtractionState.Failed
        assertThat(failure.cause).isInstanceOf(ZipArchiveException.InsufficientStorage::class.java)
        verify(extractZipEntryToFileUseCase, never())(any(), any(), anyOrNull())
    }

    @Test
    fun `test that invoke fails when the archive cannot be read`() = runTest {
        whenever(zipArchiveRepository.getArchiveIndex(ARCHIVE))
            .thenAnswer { throw ZipArchiveException.Corrupted() }

        val states = underTest(ARCHIVE, listOf("a.txt")).toList()

        assertThat((states.single() as ZipBatchExtractionState.Failed).cause)
            .isInstanceOf(ZipArchiveException.Corrupted::class.java)
    }

    @Test
    fun `test that invoke ignores a selection that is not in the archive`() = runTest {
        stub(entry(0, "a.txt"))

        val finished = underTest(ARCHIVE, listOf("gone.txt")).toList()
            .last() as ZipBatchExtractionState.Finished

        assertThat(finished.extractedPaths).isEmpty()
        assertThat(finished.failures).isEmpty()
    }

    private suspend fun stub(vararg entries: ZipArchiveEntry, freeBytes: Long = Long.MAX_VALUE) {
        val index = ZipArchiveIndex(ARCHIVE, entries.associateBy { it.path }, isEncrypted = false)
        whenever(zipArchiveRepository.getArchiveIndex(ARCHIVE)).thenReturn(index)
        whenever(zipArchiveRepository.getExtractionFreeBytes()).thenReturn(freeBytes)
        whenever(extractZipEntryToFileUseCase(any(), any(), anyOrNull()))
            .thenAnswer { invocation -> "/cache/${invocation.arguments[1]}" }
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
