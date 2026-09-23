package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveVideoPlaylistsUseCaseTest {
    private lateinit var underTest: RemoveVideoPlaylistsUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RemoveVideoPlaylistsUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            videoSectionRepository
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        val videoPlaylistIDs = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )
        whenever(videoSectionRepository.removeVideoPlaylists(videoPlaylistIDs)).thenReturn(
            videoPlaylistIDs.map { it.longValue }
        )

        val actual = underTest(videoPlaylistIDs)
        assertThat(actual).isEqualTo(videoPlaylistIDs.map { it.longValue })
    }
}