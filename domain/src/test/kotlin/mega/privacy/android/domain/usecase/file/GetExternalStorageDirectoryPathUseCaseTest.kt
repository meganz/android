package mega.privacy.android.domain.usecase.file

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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetExternalStorageDirectoryPathUseCaseTest {
    private val repository = mock<FileSystemRepository>()
    lateinit var underTest: GetExternalStorageDirectoryPathUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetExternalStorageDirectoryPathUseCase(
            fileSystemRepository = repository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that when invoked the external storage directory path is returned`() = runTest {
        val externalStorageDirectoryPath = "/storage/emulated/0"
        whenever(repository.getExternalStorageDirectoryPath()).thenReturn(
            externalStorageDirectoryPath
        )
        val actual = underTest()
        assertThat(actual).isEqualTo(externalStorageDirectoryPath)
    }
}
