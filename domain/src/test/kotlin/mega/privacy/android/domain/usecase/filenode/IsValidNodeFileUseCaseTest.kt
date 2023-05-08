package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class IsValidNodeFileUseCaseTest {

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val node = mock<DefaultTypedFileNode>()
    private val file = mock<File>()
    val underTest = IsValidNodeFileUseCase(fileSystemRepository)

    @BeforeEach
    fun resetMock() {
        reset(fileSystemRepository, node)
    }

    @Test
    fun `test that if file can not be read then it returns false`() = runTest {
        val expected = false
        whenever(file.canRead()).thenReturn(expected)
        val actual = underTest(node, file)
        assertThat(actual).isEqualTo(expected)
    }


    @Test
    fun `test that if file can be read but node size does not match file length then it returns false`() =
        runTest {
            val expected = false
            val fileLength = 1L
            val nodeSize = 2L
            whenever(file.canRead()).thenReturn(true)
            whenever(file.length()).thenReturn(fileLength)
            whenever(node.size).thenReturn(nodeSize)
            val actual = underTest(node, file)
            assertThat(actual).isEqualTo(expected)
        }


    @Test
    fun `test that if file can be read also node size matches file length but fingerprint mismatch then it returns false`() =
        runTest {
            val expected = false
            val size = 1L
            val filePath = "testPath"
            val nodeFingerPrint = "#test1"
            val fileFingerPrint = "#test2"
            whenever(file.canRead()).thenReturn(true)
            whenever(file.length()).thenReturn(size)
            whenever(file.absolutePath).thenReturn(filePath)
            whenever(node.size).thenReturn(size)
            whenever(node.fingerprint).thenReturn(nodeFingerPrint)
            whenever(fileSystemRepository.getFingerprint(filePath)).thenReturn(fileFingerPrint)
            val actual = underTest(node, file)
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that if file can be read also node size matches file length and fingerprint matches then it returns true`() =
        runTest {
            val expected = true
            val size = 1L
            val filePath = "testPath"
            val fingerPrint = "#test"
            whenever(file.canRead()).thenReturn(true)
            whenever(file.length()).thenReturn(size)
            whenever(file.absolutePath).thenReturn(filePath)
            whenever(node.size).thenReturn(size)
            whenever(node.fingerprint).thenReturn(fingerPrint)
            whenever(fileSystemRepository.getFingerprint(filePath)).thenReturn(fingerPrint)
            val actual = underTest(node, file)
            assertThat(actual).isEqualTo(expected)
        }

}