package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CacheRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFilePreviewDownloadPathUseCaseTest {
    private val cacheRepository: CacheRepository = mock()
    private val underTest = GetFilePreviewDownloadPathUseCase(cacheRepository)

    @BeforeEach
    fun resetMocks() {
        reset(cacheRepository)
    }

    @Test
    fun `test that file path is same as expected and actual`() = runTest {
        val expected = "cached path"
        whenever(cacheRepository.getPreviewDownloadPathForNode()).thenReturn(expected)
        val actual = underTest()
        assertEquals(expected, actual)
    }

    @Test
    fun `test that cache repository method is invoked when getFilePreviewDownloadPathUseCase`() =
        runTest {
            underTest()
            verify(cacheRepository).getPreviewDownloadPathForNode()
        }
}