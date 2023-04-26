package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Test class for [DeleteCameraUploadsTemporaryRootDirectoryUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteCameraUploadsTemporaryRootDirectoryUseCaseTest {

    private lateinit var underTest: DeleteCameraUploadsTemporaryRootDirectoryUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteCameraUploadsTemporaryRootDirectoryUseCase(
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest(name = "delete root directory: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the temporary root directory could be deleted`(deleteRootDirectory: Boolean) =
        runTest {
            whenever(fileSystemRepository.deleteCameraUploadsTemporaryRootDirectory()).thenReturn(
                deleteRootDirectory
            )
            assertThat(underTest()).isEqualTo(deleteRootDirectory)
        }
}