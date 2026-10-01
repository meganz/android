package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearRecentlyWatchedVideosUseCaseTest {
    private lateinit var underTest: ClearRecentlyWatchedVideosUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearRecentlyWatchedVideosUseCase(videoSectionRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that clearRecentlyWatchedVideos function returns as expected`() = runTest {
        underTest()
        verify(videoSectionRepository).clearRecentlyWatchedVideos()
    }
}