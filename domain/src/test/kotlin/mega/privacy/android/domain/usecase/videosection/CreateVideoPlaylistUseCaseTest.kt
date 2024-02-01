package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateVideoPlaylistUseCaseTest {
    private lateinit var underTest: CreateVideoPlaylistUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateVideoPlaylistUseCase(
            videoSectionRepository = videoSectionRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that the created video playlist with the right title`() = runTest {
        val expectedTitle = "video playlist title"
        val expectedVideoPlaylist = mock<VideoPlaylist> {
            on { title }.thenReturn(expectedTitle)
        }
        whenever(videoSectionRepository.createVideoPlaylist(expectedTitle)).thenReturn(
            expectedVideoPlaylist
        )

        val actual = underTest(expectedTitle)
        assertThat(actual.title).isEqualTo(expectedTitle)
    }
}