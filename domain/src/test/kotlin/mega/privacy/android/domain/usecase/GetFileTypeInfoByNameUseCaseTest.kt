package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileTypeInfoByNameUseCaseTest {
    private lateinit var underTest: GetFileTypeInfoByNameUseCase
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val name = "Name"

    @BeforeAll
    fun setUp() {
        underTest = GetFileTypeInfoByNameUseCase(fileSystemRepository = fileSystemRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the result returns correctly`() = runTest {
        val expectedFileInfoType = UnMappedFileTypeInfo("")
        whenever(fileSystemRepository.getFileTypeInfoByName(name)).thenReturn(
            expectedFileInfoType
        )
        assertThat(underTest(name)).isEqualTo(expectedFileInfoType)
    }

    @Test
    fun `test that the function is invoked as expected`() = runTest {
        underTest(name)
        verify(fileSystemRepository).getFileTypeInfoByName(name)
    }
}