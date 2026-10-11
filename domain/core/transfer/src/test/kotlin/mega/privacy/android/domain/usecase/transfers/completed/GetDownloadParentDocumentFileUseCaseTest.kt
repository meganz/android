package mega.privacy.android.domain.usecase.transfers.completed

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDownloadParentDocumentFileUseCaseTest {

    private lateinit var underTest: GetDownloadParentDocumentFileUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetDownloadParentDocumentFileUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that use case invokes repository and returns DocumentEntity`() = runTest {
        val fileStringUri = "content://example/documents"
        val expected = mock<DocumentEntity>()

        whenever(fileSystemRepository.getDocumentFileIfContentUri(fileStringUri)) doReturn expected

        assertThat(underTest(fileStringUri)).isEqualTo(expected)

        verify(fileSystemRepository).getDocumentFileIfContentUri(fileStringUri)
    }

    @Test
    fun `test that use case invokes repository and returns null`() = runTest {
        val fileStringUri = "content://example/documents"

        whenever(fileSystemRepository.getDocumentFileIfContentUri(fileStringUri)) doReturn null

        assertThat(underTest(fileStringUri)).isNull()

        verify(fileSystemRepository).getDocumentFileIfContentUri(fileStringUri)
    }
}