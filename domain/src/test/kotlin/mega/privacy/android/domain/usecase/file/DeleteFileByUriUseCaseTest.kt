package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteFileByUriUseCaseTest {

    private val repository: FileSystemRepository = mock()
    private lateinit var underTest: DeleteFileByUriUseCase

    @BeforeAll
    fun setUp() {
        underTest = DeleteFileByUriUseCase(repository)
    }

    @BeforeEach
    fun reset() {
        reset(repository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that delete file by uri returns correctly`(expected: Boolean) = runTest {
        val testUri = "file://test/file/path"

        whenever(repository.deleteFileByUri(testUri)).thenReturn(expected)
        Truth.assertThat(underTest(testUri)).isEqualTo(expected)
    }
}