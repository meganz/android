package mega.privacy.android.feature.sync.domain.stalledissue

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.RenameFilesWithTheSameNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RenameFilesWithTheSameNameUseCaseTest {

    @TempDir
    lateinit var temporaryFolder: File

    private val underTest = RenameFilesWithTheSameNameUseCase()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that renaming files with same name adds a counter`() = runTest {
        val file1 = File(temporaryFolder, "f%301.txt").apply { createNewFile() }
        val file2 = File(temporaryFolder, "f01.txt").apply { createNewFile() }
        val filePaths = listOf(file1.absolutePath, file2.absolutePath)

        underTest(filePaths)

        val renamedFile1 = File(temporaryFolder, "f%301.txt")
        val renamedFile2 = File(temporaryFolder, "f01 (1).txt")
        Truth.assertThat(renamedFile1.exists()).isTrue()
        Truth.assertThat(renamedFile2.exists()).isTrue()
    }

    @Test
    fun `test that renaming folders with same name adds a counter`() = runTest {
        val file1 =
            File(temporaryFolder, "f%301").apply { createNewFile() }
        val file2 =
            File(temporaryFolder, "f01").apply { createNewFile() }
        val filePaths = listOf(file1.absolutePath, file2.absolutePath)

        underTest(filePaths)

        val renamedFile1 = File(temporaryFolder, "f%301")
        val renamedFile2 = File(temporaryFolder, "f01 (1)")
        Truth.assertThat(renamedFile1.exists()).isTrue()
        Truth.assertThat(renamedFile2.exists()).isTrue()
    }
}
