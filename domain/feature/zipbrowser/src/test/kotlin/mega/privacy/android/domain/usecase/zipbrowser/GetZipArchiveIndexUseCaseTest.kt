package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.repository.ZipArchiveRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetZipArchiveIndexUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val underTest = GetZipArchiveIndexUseCase(zipArchiveRepository)

    @BeforeEach
    fun resetMocks() = reset(zipArchiveRepository)

    @Test
    fun `test that invoke returns the index from the repository`() = runTest {
        val expected = ZipArchiveIndex(ARCHIVE, emptyMap(), isEncrypted = false)
        whenever(zipArchiveRepository.getArchiveIndex(ARCHIVE)).thenReturn(expected)

        assertThat(underTest(ARCHIVE)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke propagates Corrupted when the archive cannot be read`() = runTest {
        // thenAnswer rather than thenThrow: Mockito rejects a checked exception that the mocked
        // signature does not declare, and Kotlin declares none.
        whenever(zipArchiveRepository.getArchiveIndex(ARCHIVE))
            .thenAnswer { throw ZipArchiveException.Corrupted() }

        assertThrows<ZipArchiveException.Corrupted> { underTest(ARCHIVE) }
    }

    private companion object {
        const val ARCHIVE = "/tmp/a.zip"
    }
}
