package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFilesInDocumentFolderUseCaseTest {
    private val repository = mock<FileSystemRepository>()
    lateinit var underTest: GetFilesInDocumentFolderUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetFilesInDocumentFolderUseCase(
            fileSystemRepository = repository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that files are returned when invoked`() = runTest {
        val folder = UriPath("folder")
        val entity = mock<DocumentEntity>()
        val expected = DocumentFolder(listOf(entity))
        whenever(repository.getFilesInDocumentFolder(folder)).thenReturn(expected)
        val actual = underTest(folder)
        assertThat(actual).isEqualTo(expected)
    }
}