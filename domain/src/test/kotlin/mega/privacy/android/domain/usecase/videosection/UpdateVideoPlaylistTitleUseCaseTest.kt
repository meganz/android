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
class UpdateVideoPlaylistTitleUseCaseTest {
    private lateinit var underTest: UpdateVideoPlaylistTitleUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateVideoPlaylistTitleUseCase(
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
        val newTitle = "newTitle"
        whenever(videoSectionRepository.updateVideoPlaylistTitle(NodeId(1L), newTitle))
            .thenReturn(newTitle)

        val actual = underTest(NodeId(1L), newTitle)
        assertThat(actual).isEqualTo(newTitle)
    }
}