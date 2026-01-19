package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateVideoPlaylistTitleUseCaseTest {
    private lateinit var underTest: UpdateVideoPlaylistTitleUseCase
    private val validatePlaylistNameUseCase = mock<ValidatePlaylistNameUseCase>()
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateVideoPlaylistTitleUseCase(
            validatePlaylistNameUseCase = validatePlaylistNameUseCase,
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            validatePlaylistNameUseCase,
            videoSectionRepository
        )
    }

    @Test
    fun `test that playlist title is updated successfully when validation passes`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "newTitle"
        whenever(validatePlaylistNameUseCase(newTitle)).thenReturn(Unit)
        whenever(videoSectionRepository.updateVideoPlaylistTitle(playlistId, newTitle))
            .thenReturn(newTitle)

        val actual = underTest(playlistId, newTitle)

        verify(validatePlaylistNameUseCase).invoke(newTitle)
        verify(videoSectionRepository).updateVideoPlaylistTitle(playlistId, newTitle)
        assertThat(actual).isEqualTo(newTitle)
    }

    @Test
    fun `test that playlist is not updated when Empty exception is thrown`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "   "
        whenever(validatePlaylistNameUseCase(any())).thenThrow(PlaylistNameValidationException.Empty)

        assertThrows<PlaylistNameValidationException.Empty> {
            underTest(playlistId, newTitle)
        }

        verify(validatePlaylistNameUseCase).invoke(newTitle)
        verify(videoSectionRepository, never()).updateVideoPlaylistTitle(any(), any())
    }

    @Test
    fun `test that playlist is not updated when Exists exception is thrown`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "Existing Playlist"
        whenever(validatePlaylistNameUseCase(any())).thenThrow(PlaylistNameValidationException.Exists)

        assertThrows<PlaylistNameValidationException.Exists> {
            underTest(playlistId, newTitle)
        }

        verify(validatePlaylistNameUseCase).invoke(newTitle)
        verify(videoSectionRepository, never()).updateVideoPlaylistTitle(any(), any())
    }

    @Test
    fun `test that playlist is not updated when InvalidCharacters exception is thrown`() =
        runTest {
            val playlistId = NodeId(1L)
            val newTitle = "My<Playlist>"
            whenever(validatePlaylistNameUseCase(any())).thenThrow(
                PlaylistNameValidationException.InvalidCharacters
            )

            assertThrows<PlaylistNameValidationException.InvalidCharacters> {
                underTest(playlistId, newTitle)
            }

            verify(validatePlaylistNameUseCase).invoke(newTitle)
            verify(videoSectionRepository, never()).updateVideoPlaylistTitle(any(), any())
        }
}