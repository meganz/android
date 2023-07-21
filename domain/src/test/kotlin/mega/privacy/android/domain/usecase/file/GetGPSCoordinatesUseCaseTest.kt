package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetGPSCoordinatesUseCaseTest {

    private lateinit var underTest: GetGPSCoordinatesUseCase

    private lateinit var fileSystemRepository: FileSystemRepository

    @BeforeAll
    fun setUp() {
        fileSystemRepository = mock()
        underTest = GetGPSCoordinatesUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the GPS coordinates of file type video are retrieved`() =
        runTest {
            val result = Pair(6F, 9F)
            whenever(fileSystemRepository.getVideoGPSCoordinates(any())).thenReturn(result)
            Truth.assertThat(underTest("", true)).isEqualTo(result)
        }

    @Test
    fun `test that the GPS coordinates of file type photo are retrieved`() =
        runTest {
            val result = Pair(6F, 9F)
            whenever(fileSystemRepository.getPhotoGPSCoordinates(any())).thenReturn(result)
            Truth.assertThat(underTest("", false)).isEqualTo(result)
        }
}