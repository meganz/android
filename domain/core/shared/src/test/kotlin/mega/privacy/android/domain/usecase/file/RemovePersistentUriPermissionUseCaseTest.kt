package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [RemovePersistentUriPermissionUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemovePersistentUriPermissionUseCaseTest {

    private lateinit var underTest: RemovePersistentUriPermissionUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = RemovePersistentUriPermissionUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the use case invokes the removePersistentPermission method of fileSystemRepository`() =
        runTest {
            val uriPath = UriPath("originalUriPath")
            underTest(uriPath)
            verify(fileSystemRepository).removePersistentPermission(uriPath)
        }
}
