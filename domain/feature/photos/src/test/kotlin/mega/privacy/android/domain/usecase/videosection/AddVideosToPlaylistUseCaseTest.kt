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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddVideosToPlaylistUseCaseTest {
    private lateinit var underTest: AddVideosToPlaylistUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AddVideosToPlaylistUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that the add videos return the correct number of added videos`() = runTest {
        val testPlaylistID = NodeId(1L)
        val testVideoIDs = listOf(NodeId(1L), NodeId(2L), NodeId(3L))
        whenever(
            videoSectionRepository.addVideosToPlaylist(
                testPlaylistID,
                testVideoIDs
            )
        ).thenReturn(
            testVideoIDs.size
        )

        val actual = underTest(testPlaylistID, testVideoIDs)
        assertThat(actual).isEqualTo(testVideoIDs.size)
    }
}