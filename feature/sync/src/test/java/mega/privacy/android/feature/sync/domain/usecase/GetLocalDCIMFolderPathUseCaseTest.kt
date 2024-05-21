package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth
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
internal class GetLocalDCIMFolderPathUseCaseTest {
    private lateinit var underTest: GetLocalDCIMFolderPathUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetLocalDCIMFolderPathUseCase(fileSystemRepository = fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the local DCIM folder path is retrieved`() = runTest {
        val testPath = "/storage/emulated/0/DCIM"

        whenever(fileSystemRepository.localDCIMFolderPath).thenReturn(testPath)
        Truth.assertThat(underTest.invoke()).isEqualTo(testPath)
    }
}