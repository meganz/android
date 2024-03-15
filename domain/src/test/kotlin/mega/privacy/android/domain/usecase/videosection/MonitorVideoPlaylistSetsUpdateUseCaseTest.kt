package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorVideoPlaylistSetsUpdateUseCaseTest {
    private lateinit var underTest: MonitorVideoPlaylistSetsUpdateUseCase

    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorVideoPlaylistSetsUpdateUseCase(videoSectionRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that invoke returns when video player list sets is updated`() {
        val expectedResult = flowOf(listOf(1L, 2L, 3L))
        whenever(videoSectionRepository.monitorVideoPlaylistSetsUpdate()).thenReturn(expectedResult)
        assertThat(underTest.invoke()).isEqualTo(expectedResult)
    }
}