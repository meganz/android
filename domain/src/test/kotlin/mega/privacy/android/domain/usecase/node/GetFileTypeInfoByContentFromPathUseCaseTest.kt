package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileTypeInfoByContentFromPathUseCaseTest {
    private lateinit var underTest: GetFileTypeInfoByContentFromPathUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val headerSize = GetFileTypeInfoByContentUseCase.HEADER_SIZE_BYTES

    @BeforeEach
    fun setUp() {
        underTest = GetFileTypeInfoByContentFromPathUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the type is detected from the local file header`() = runTest {
        val path = "/local/path/file"
        val header = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        whenever(fileSystemRepository.readFirstBytesFromPath(path, headerSize)).thenReturn(header)
        whenever(fileSystemRepository.getFileTypeInfoFromContent(header, 0))
            .thenReturn(TextFileTypeInfo("text/plain", ""))

        val result = underTest(path)

        assertThat(result).isEqualTo(TextFileTypeInfo("text/plain", ""))
    }

    @Test
    fun `test that null is returned when the header is empty`() = runTest {
        val path = "/local/path/file"
        whenever(fileSystemRepository.readFirstBytesFromPath(path, headerSize))
            .thenReturn(ByteArray(0))

        assertThat(underTest(path)).isNull()
    }

    @Test
    fun `test that null is returned when the file cannot be read`() = runTest {
        val path = "/local/path/file"
        whenever(fileSystemRepository.readFirstBytesFromPath(path, headerSize)).thenReturn(null)

        assertThat(underTest(path)).isNull()
    }
}
