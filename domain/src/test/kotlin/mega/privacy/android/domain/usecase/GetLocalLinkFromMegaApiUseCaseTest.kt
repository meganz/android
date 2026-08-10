package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.StreamingServerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalLinkFromMegaApiUseCaseTest {
    private lateinit var underTest: GetLocalLinkFromMegaApiUseCase
    private val streamingServerRepository = mock<StreamingServerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetLocalLinkFromMegaApiUseCase(streamingServerRepository = streamingServerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(streamingServerRepository)
    }

    @Test
    fun `test that result is null when the local link is null`() =
        runTest {
            val testHandle = 123456L
            whenever(streamingServerRepository.getFileStreamingUri(testHandle)).thenReturn(null)
            Truth.assertThat(underTest(testHandle)).isNull()
        }

    @Test
    fun `test that the local link is returned`() =
        runTest {
            val testHandle = 123456L
            val testLink = "expected link"
            whenever(streamingServerRepository.getFileStreamingUri(testHandle)).thenReturn(testLink)
            Truth.assertThat(underTest(testHandle)).isEqualTo(testLink)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            val testHandle = 123456L
            underTest(testHandle)
            verify(streamingServerRepository).getFileStreamingUri(testHandle)
        }
}