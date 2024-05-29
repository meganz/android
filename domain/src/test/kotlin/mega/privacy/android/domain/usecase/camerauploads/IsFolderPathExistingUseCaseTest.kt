package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsFolderPathExistingUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsFolderPathExistingUseCaseTest {

    private lateinit var underTest: IsFolderPathExistingUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsFolderPathExistingUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the folder does not exist if the path is null`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @Test
    fun `test that the folder does not exist if the path is empty`() = runTest {
        assertThat(underTest("")).isFalse()
    }

    @Test
    fun `test that the folder does not exist if the path only contains whitespaces`() =
        runTest {
            assertThat(underTest(" ")).isFalse()
        }

    @Test
    fun `test that the folder does not exist if the file system cannot find it`() = runTest {
        val folderPath = "folder/path"

        whenever(fileSystemRepository.doesFolderExists(folderPath)).thenReturn(false)

        assertThat(underTest(folderPath)).isFalse()
    }

    @Test
    fun `test that the folder exists`() = runTest {
        val folderPath = "folder/path"

        whenever(fileSystemRepository.doesFolderExists(folderPath)).thenReturn(true)

        assertThat(underTest(folderPath)).isTrue()
    }
}