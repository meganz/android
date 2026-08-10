package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasSuitableAppToOpenFileUseCaseTest {

    private val fileSystemRepository = mock<FileSystemRepository>()

    private val underTest = HasSuitableAppToOpenFileUseCase(fileSystemRepository)

    @ParameterizedTest(name = "and repository returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the use case returns the repository result`(expected: Boolean) = runTest {
        val mimeType = "application/pdf"
        fileSystemRepository.stub {
            on { hasSuitableAppToOpenFile(mimeType) }.thenReturn(expected)
        }

        assertThat(underTest(mimeType)).isEqualTo(expected)
    }
}
