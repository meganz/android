package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
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
class CreateVideoPlaylistUseCaseTest {
    private lateinit var underTest: CreateVideoPlaylistUseCase
    private val validatePlaylistNameUseCase = mock<ValidatePlaylistNameUseCase>()
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateVideoPlaylistUseCase(
            validatePlaylistNameUseCase = validatePlaylistNameUseCase,
            videoSectionRepository = videoSectionRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(validatePlaylistNameUseCase, videoSectionRepository)
    }

    @Test
    fun `test that playlist is created successfully when validation passes`() = runTest {
        val expectedTitle = "video playlist title"
        val expectedVideoPlaylist = mock<UserVideoPlaylist> {
            on { title }.thenReturn(expectedTitle)
        }
        whenever(validatePlaylistNameUseCase(expectedTitle)).thenReturn(Unit)
        whenever(videoSectionRepository.createVideoPlaylist(expectedTitle)).thenReturn(
            expectedVideoPlaylist
        )

        val actual = underTest(expectedTitle)

        verify(validatePlaylistNameUseCase).invoke(expectedTitle)
        verify(videoSectionRepository).createVideoPlaylist(expectedTitle)
        assertThat((actual as? UserVideoPlaylist)?.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `test that playlist is not created when Empty exception is thrown`() = runTest {
        val title = "   "
        whenever(validatePlaylistNameUseCase(any())).thenThrow(PlaylistNameValidationException.Empty)

        assertThrows<PlaylistNameValidationException.Empty> {
            underTest(title)
        }

        verify(validatePlaylistNameUseCase).invoke(title)
        verify(videoSectionRepository, never()).createVideoPlaylist(any())
    }

    @Test
    fun `test that playlist is not created when Exists exception is thrown`() = runTest {
        val title = "Existing Playlist"
        whenever(validatePlaylistNameUseCase(any())).thenThrow(PlaylistNameValidationException.Exists)

        assertThrows<PlaylistNameValidationException.Exists> {
            underTest(title)
        }

        verify(validatePlaylistNameUseCase).invoke(title)
        verify(videoSectionRepository, never()).createVideoPlaylist(any())
    }

    @Test
    fun `test that playlist is not created when InvalidCharacters exception is thrown`() = runTest {
        val title = "My<Playlist>"
        whenever(validatePlaylistNameUseCase(any())).thenThrow(
            PlaylistNameValidationException.InvalidCharacters
        )

        assertThrows<PlaylistNameValidationException.InvalidCharacters> {
            underTest(title)
        }

        verify(validatePlaylistNameUseCase).invoke(title)
        verify(videoSectionRepository, never()).createVideoPlaylist(any())
    }
}