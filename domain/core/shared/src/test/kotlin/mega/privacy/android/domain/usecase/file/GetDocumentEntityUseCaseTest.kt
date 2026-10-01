package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDocumentEntityUseCaseTest {

    private lateinit var underTest: GetDocumentEntityUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetDocumentEntityUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun cleanUp() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that when use case is invoked it returns repository value`(
        isNull: Boolean,
    ) = runTest {
        val uriPath = UriPath("foo")
        val expected = mock<DocumentEntity>().takeIf { isNull.not() }
        whenever(fileSystemRepository.getDocumentEntity(uriPath)) doReturn expected

        val actual = underTest(uriPath)
        if (isNull) {
            assertThat(actual).isNull()
        } else {
            assertThat(actual).isEqualTo(expected)
        }
    }
}