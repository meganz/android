package mega.privacy.android.domain.usecase.file

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchFilesInDocumentFolderRecursiveUseCaseTest {
    private val fileSystemRepository: FileSystemRepository = mock()
    private val underTest = SearchFilesInDocumentFolderRecursiveUseCase(fileSystemRepository)

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that files are returned when invoked`() = runTest {
        val folder = UriPath("folder")
        val query = "query"
        val entity = mock<DocumentEntity>()
        val expected = DocumentFolder(listOf(entity))
        whenever(
            fileSystemRepository.searchFilesInDocumentFolderRecursive(
                folder,
                query
            )
        ).thenReturn(flowOf(expected))
        underTest(folder, query).test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }
}