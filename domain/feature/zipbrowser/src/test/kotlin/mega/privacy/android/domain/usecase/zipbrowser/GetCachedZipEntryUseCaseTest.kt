package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCachedZipEntryUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val underTest = GetCachedZipEntryUseCase(zipArchiveRepository)

    @BeforeEach
    fun resetMocks() = reset(zipArchiveRepository)

    @Test
    fun `test that invoke returns the cached path when the entry was extracted before`() =
        runTest {
            whenever(zipArchiveRepository.getCachedEntryPath(ARCHIVE, ENTRY)).thenReturn(CACHED)

            assertThat(underTest(ARCHIVE, ENTRY)).isEqualTo(CACHED)
        }

    @Test
    fun `test that invoke returns null when the entry was not extracted before`() = runTest {
        whenever(zipArchiveRepository.getCachedEntryPath(ARCHIVE, ENTRY)).thenReturn(null)

        assertThat(underTest(ARCHIVE, ENTRY)).isNull()
    }

    private companion object {
        const val ARCHIVE = "/storage/a.zip"
        const val ENTRY = "docs/a.txt"
        const val CACHED = "/cache/a.zip/docs/a.txt"
    }
}
