package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistsUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistsUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistsUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            videoSectionRepository
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videoPlaylists is not empty`() = runTest {
        val list = listOf(mock<VideoPlaylist>())
        whenever(videoSectionRepository.getVideoPlaylists()).thenReturn(list)
        assertThat(underTest()).isNotEmpty()
    }

    @Test
    fun `test that videoPlaylists is empty`() =
        runTest {
            whenever(videoSectionRepository.getVideoPlaylists()).thenReturn(emptyList())
            assertThat(underTest()).isEmpty()
        }
}