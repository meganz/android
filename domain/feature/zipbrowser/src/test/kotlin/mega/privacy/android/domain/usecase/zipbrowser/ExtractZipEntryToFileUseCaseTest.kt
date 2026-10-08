package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractZipEntryToFileUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val underTest = ExtractZipEntryToFileUseCase(zipArchiveRepository)

    @BeforeEach
    fun resetMocks() = reset(zipArchiveRepository)

    @Test
    fun `test that invoke returns the extracted path when the extraction completes`() = runTest {
        whenever(zipArchiveRepository.extractEntry(ARCHIVE, ENTRY, null)).thenReturn(
            flowOf(
                ZipExtractionState.InProgress(10L, 20L),
                ZipExtractionState.Completed(EXTRACTED),
            )
        )

        assertThat(underTest(ARCHIVE, ENTRY)).isEqualTo(EXTRACTED)
    }

    @Test
    fun `test that invoke throws the cause when the extraction fails`() = runTest {
        val cause = ZipArchiveException.EntryExtractionFailed(ENTRY)
        whenever(zipArchiveRepository.extractEntry(ARCHIVE, ENTRY, PASSWORD))
            .thenReturn(flowOf(ZipExtractionState.Failed(cause)))

        val thrown = assertThrows<ZipArchiveException.EntryExtractionFailed> {
            underTest(ARCHIVE, ENTRY, PASSWORD)
        }

        assertThat(thrown).isSameInstanceAs(cause)
    }

    @Test
    fun `test that invoke throws EntryExtractionFailed when the extraction ends without a result`() =
        runTest {
            whenever(zipArchiveRepository.extractEntry(ARCHIVE, ENTRY, null))
                .thenReturn(flowOf(ZipExtractionState.InProgress(10L, 20L)))

            assertThrows<ZipArchiveException.EntryExtractionFailed> { underTest(ARCHIVE, ENTRY) }
        }

    private companion object {
        const val ARCHIVE = "/storage/a.zip"
        const val ENTRY = "docs/a.txt"
        const val EXTRACTED = "/cache/a.zip/docs/a.txt"
        val PASSWORD = "secret".toCharArray()
    }
}
