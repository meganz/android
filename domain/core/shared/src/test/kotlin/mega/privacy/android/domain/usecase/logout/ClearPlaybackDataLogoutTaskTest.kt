package mega.privacy.android.domain.usecase.logout

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ClearPlaybackDataLogoutTaskTest {
    private lateinit var underTest: ClearPlaybackDataLogoutTask

    private val clearRecentlyWatchedVideosUseCase = mock<ClearRecentlyWatchedVideosUseCase>()
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeEach
    internal fun setUp() {
        reset(clearRecentlyWatchedVideosUseCase, mediaPlayerRepository)
        underTest = ClearPlaybackDataLogoutTask(
            clearRecentlyWatchedVideosUseCase = clearRecentlyWatchedVideosUseCase,
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @Test
    fun `test that onLogoutSuccess clears the recently watched videos`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearRecentlyWatchedVideosUseCase).invoke()
    }

    @Test
    fun `test that onLogoutSuccess clears the stored media playback infos`() = runTest {
        underTest.onLogoutSuccess()

        verify(mediaPlayerRepository).clearAllPlaybackInfos()
    }

    @Test
    fun `test that onLogoutSuccess clears the legacy playback information`() = runTest {
        underTest.onLogoutSuccess()

        verify(mediaPlayerRepository).clearPlaybackInformation()
    }

    @Test
    fun `test that onLogoutSuccess runs the remaining cleanups when the first one fails`() =
        runTest {
            whenever(clearRecentlyWatchedVideosUseCase()).thenThrow(RuntimeException("boom"))

            assertThrows<RuntimeException> { underTest.onLogoutSuccess() }

            verify(mediaPlayerRepository).clearAllPlaybackInfos()
            verify(mediaPlayerRepository).clearPlaybackInformation()
        }

    @Test
    fun `test that onLogoutSuccess runs the last cleanup when an earlier one fails`() = runTest {
        whenever(mediaPlayerRepository.clearAllPlaybackInfos()).thenThrow(RuntimeException("boom"))

        assertThrows<RuntimeException> { underTest.onLogoutSuccess() }

        verify(clearRecentlyWatchedVideosUseCase).invoke()
        verify(mediaPlayerRepository).clearPlaybackInformation()
    }

    @Test
    fun `test that onLogoutSuccess suppresses the later failure when more than one cleanup fails`() =
        runTest {
            val videosFailure = RuntimeException("videos failed")
            val playbackInfosFailure = RuntimeException("playback infos failed")
            whenever(clearRecentlyWatchedVideosUseCase()).thenThrow(videosFailure)
            whenever(mediaPlayerRepository.clearAllPlaybackInfos()).thenThrow(playbackInfosFailure)

            val thrown = assertThrows<RuntimeException> { underTest.onLogoutSuccess() }

            assertThat(thrown).isEqualTo(videosFailure)
            assertThat(thrown.suppressed.toList()).containsExactly(playbackInfosFailure)
        }
}
