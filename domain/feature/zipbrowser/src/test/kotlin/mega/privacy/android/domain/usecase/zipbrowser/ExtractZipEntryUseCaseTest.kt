package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractZipEntryUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val underTest = ExtractZipEntryUseCase(zipArchiveRepository)

    @BeforeEach
    fun resetMocks() = reset(zipArchiveRepository)

    @Test
    fun `test that invoke emits the extraction states from the repository`() = runTest {
        val states = listOf(
            ZipExtractionState.InProgress(10L, 20L),
            ZipExtractionState.Completed("/cache/a.zip/docs/a.txt"),
        )
        whenever(zipArchiveRepository.extractEntry(ARCHIVE, ENTRY, null))
            .thenReturn(flowOf(*states.toTypedArray()))

        assertThat(underTest(ARCHIVE, ENTRY).toList()).isEqualTo(states)
    }

    @Test
    fun `test that invoke passes the password to the repository`() = runTest {
        val password = "secret".toCharArray()
        val completed = ZipExtractionState.Completed("/cache/a.zip/docs/a.txt")
        whenever(zipArchiveRepository.extractEntry(ARCHIVE, ENTRY, password))
            .thenReturn(flowOf(completed))

        assertThat(underTest(ARCHIVE, ENTRY, password).toList()).containsExactly(completed)
    }

    private companion object {
        const val ARCHIVE = "/storage/a.zip"
        const val ENTRY = "docs/a.txt"
    }
}
