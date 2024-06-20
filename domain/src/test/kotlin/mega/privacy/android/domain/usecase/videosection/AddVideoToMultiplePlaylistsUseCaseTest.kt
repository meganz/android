package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddVideoToMultiplePlaylistsUseCaseTest {
    private lateinit var underTest: AddVideoToMultiplePlaylistsUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    private val testPlaylistIDs = listOf(1L, 2L, 3L)
    private val testVideoId = 0L

    @BeforeAll
    fun setUp() {
        underTest = AddVideoToMultiplePlaylistsUseCase(
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that the result is correct`() = runTest {
        whenever(
            videoSectionRepository.addVideoToMultiplePlaylists(
                testPlaylistIDs,
                testVideoId
            )
        ).thenReturn(
            testPlaylistIDs
        )

        val actual = underTest(testPlaylistIDs, testVideoId)
        assertThat(actual.size).isEqualTo(3)
    }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testPlaylistIDs, testVideoId)
            verify(videoSectionRepository).addVideoToMultiplePlaylists(testPlaylistIDs, testVideoId)
        }
}