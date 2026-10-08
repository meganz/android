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
class ValidateZipPasswordUseCaseTest {

    private val zipArchiveRepository = mock<ZipArchiveRepository>()
    private val underTest = ValidateZipPasswordUseCase(zipArchiveRepository)

    @BeforeEach
    fun resetMocks() = reset(zipArchiveRepository)

    @Test
    fun `test that invoke returns true when the repository accepts the password`() = runTest {
        whenever(zipArchiveRepository.isPasswordValid(ARCHIVE, PASSWORD)).thenReturn(true)

        assertThat(underTest(ARCHIVE, PASSWORD)).isTrue()
    }

    @Test
    fun `test that invoke returns false when the repository rejects the password`() = runTest {
        whenever(zipArchiveRepository.isPasswordValid(ARCHIVE, PASSWORD)).thenReturn(false)

        assertThat(underTest(ARCHIVE, PASSWORD)).isFalse()
    }

    private companion object {
        const val ARCHIVE = "/storage/a.zip"
        val PASSWORD = "secret".toCharArray()
    }
}
