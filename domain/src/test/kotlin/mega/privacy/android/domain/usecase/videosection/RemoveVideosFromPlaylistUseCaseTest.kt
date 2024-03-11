package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveVideosFromPlaylistUseCaseTest {
    private lateinit var underTest: RemoveVideosFromPlaylistUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RemoveVideosFromPlaylistUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that the removed videos return the correct result`() = runTest {
        val testPlaylistID = NodeId(1L)
        val testVideoElementIDs = listOf(1L, 2L, 3L)
        val result = listOf(1L, 2L, 3L)
        whenever(
            videoSectionRepository.removeVideosFromPlaylist(testPlaylistID, testVideoElementIDs)
        ).thenReturn(result.size)

        val actual = underTest(testPlaylistID, testVideoElementIDs)
        assertThat(actual).isEqualTo(3)
    }
}