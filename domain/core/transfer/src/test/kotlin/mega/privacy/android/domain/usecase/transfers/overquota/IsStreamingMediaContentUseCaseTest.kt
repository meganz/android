package mega.privacy.android.domain.usecase.transfers.overquota

import com.google.common.truth.Truth.assertThat
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
class IsStreamingMediaContentUseCaseTest {

    private lateinit var underTest: IsStreamingMediaContentUseCase

    private val streamingServerRepository = mock<StreamingServerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsStreamingMediaContentUseCase(
            streamingServerRepository = streamingServerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(streamingServerRepository)
    }

    @Test
    fun `test that invoke returns true when the last streamed content is media`() {
        whenever(streamingServerRepository.isLastStreamedContentMedia()).thenReturn(true)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that invoke returns false when the last streamed content is not media`() {
        whenever(streamingServerRepository.isLastStreamedContentMedia()).thenReturn(false)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke calls isLastStreamedContentMedia`() {
        underTest()

        verify(streamingServerRepository).isLastStreamedContentMedia()
    }
}
