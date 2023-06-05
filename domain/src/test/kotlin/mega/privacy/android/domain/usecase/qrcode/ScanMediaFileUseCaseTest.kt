package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ScanMediaFileUseCaseTest {

    private lateinit var underTest: ScanMediaFileUseCase

    private val fileSystemRepository: FileSystemRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = ScanMediaFileUseCase(fileSystemRepository = fileSystemRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(fileSystemRepository)
    }

    @Test
    internal fun `test that scan media file method in file system repository is invoked`() {
        val expectedFileList = arrayOf("file1.jpg", "file2.jpg")
        val expectedMimeTypeList = arrayOf("image/jpeg", "image/jpeg")

        underTest(expectedFileList, expectedMimeTypeList)
        verify(fileSystemRepository).scanMediaFile(expectedFileList, expectedMimeTypeList)
    }
}
