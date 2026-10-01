package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflineFileTotalSizeUseCaseTest {
    private lateinit var underTest: GetOfflineFileTotalSizeUseCase
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineFileTotalSizeUseCase(
            fileSystemRepository = fileSystemRepository,
        )
    }

    @Test
    fun `test that file size is returned when invoked`() =
        runTest {
            val expectedSize = 1000L
            val file = mock<File>()
            whenever(fileSystemRepository.getTotalSize(file)).thenReturn(expectedSize)
            val actual = underTest(file)
            assertThat(actual).isEqualTo(expectedSize)
        }

}