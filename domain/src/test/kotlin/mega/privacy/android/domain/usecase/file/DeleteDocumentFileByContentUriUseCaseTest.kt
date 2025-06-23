package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
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
class DeleteDocumentFileByContentUriUseCaseTest {

    private val repository: FileSystemRepository = mock()
    private lateinit var underTest: DeleteDocumentFileByContentUriUseCase

    @BeforeAll
    fun setUp() {
        underTest = DeleteDocumentFileByContentUriUseCase(repository)
    }

    @BeforeEach
    fun reset() {
        reset(repository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that delete a document file  by content uri returns correctly`(expected: Boolean) =
        runTest {
            val testUriPath = UriPath("content://test/file/path")
            whenever(repository.deleteDocumentFileByContentUri(testUriPath)).thenReturn(expected)
            Truth.assertThat(underTest(testUriPath)).isEqualTo(expected)
        }
}
