package mega.privacy.android.domain.usecase.transfers.completed

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDownloadDocumentFileUseCaseTest {

    private lateinit var underTest: GetDownloadDocumentFileUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetDownloadDocumentFileUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that use case invokes repository and returns DocumentEntity`() = runTest {
        val fileStringUri = "content://example/document.pdf"
        val fileName = "document.pdf"
        val expected = mock<DocumentEntity>()

        whenever(fileSystemRepository.getDocumentFileIfContentUri(fileStringUri, fileName)) doReturn expected

        assertThat(underTest(fileStringUri, fileName)).isEqualTo(expected)

        verify(fileSystemRepository).getDocumentFileIfContentUri(fileStringUri, fileName)
    }

    @Test
    fun `test that use case invokes repository and returns null`() = runTest {
        val fileStringUri = "content://example/document.pdf"
        val fileName = "document.pdf"

        whenever(fileSystemRepository.getDocumentFileIfContentUri(fileStringUri, fileName)) doReturn null

        assertThat(underTest(fileStringUri, fileName)).isNull()

        verify(fileSystemRepository).getDocumentFileIfContentUri(fileStringUri, fileName)
    }
}