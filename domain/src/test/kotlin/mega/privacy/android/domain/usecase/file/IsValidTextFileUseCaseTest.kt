package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsValidTextFileUseCaseTest {

    private lateinit var underTest: IsValidTextFileUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsValidTextFileUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that invoke does not throw when fileName is a valid text file`() {
        val fileName = "file.txt"
        whenever(fileSystemRepository.getFileTypeInfoByName(fileName)).thenReturn(
            TextFileTypeInfo(mimeType = "text/plain", extension = "txt")
        )

        assertDoesNotThrow { underTest(fileName) }
    }

    @Test
    fun `test that invoke throws InvalidNodeExtensionException when fileName is not a valid text file`() {
        val fileName = "file.exe"
        whenever(fileSystemRepository.getFileTypeInfoByName(fileName)).thenReturn(
            UnknownFileTypeInfo(mimeType = "", extension = "")
        )

        assertThrows<InvalidNodeExtensionException> { underTest(fileName) }
    }

    @Test
    fun `test that invoke with Node does not throw when node name is a valid text file`() {
        val node = mock<Node> {
            on { name }.thenReturn("notes.txt")
        }
        whenever(fileSystemRepository.getFileTypeInfoByName("notes.txt")).thenReturn(
            TextFileTypeInfo(mimeType = "text/plain", extension = "txt")
        )

        assertDoesNotThrow { underTest(node) }
    }

    @Test
    fun `test that invoke with Node throws InvalidNodeExtensionException when node name is not a valid text file`() {
        val node = mock<Node> {
            on { name }.thenReturn("image.png")
        }
        whenever(fileSystemRepository.getFileTypeInfoByName("image.png")).thenReturn(
            UnknownFileTypeInfo(mimeType = "", extension = "")
        )

        assertThrows<InvalidNodeExtensionException> { underTest(node) }
    }
}
