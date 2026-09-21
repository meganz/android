package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.model.AudioControllerState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AudioMiniPlayerViewModelTest {

    private lateinit var underTest: AudioMiniPlayerViewModel
    private lateinit var playerStateFlow: MutableSharedFlow<AudioControllerState>

    private val gateway = mock<AudioMediaControllerGateway>()

    @BeforeEach
    fun setUp() {
        reset(gateway)
        playerStateFlow = MutableSharedFlow(replay = 1)
        whenever(gateway.playerState).thenReturn(playerStateFlow)
        underTest = AudioMiniPlayerViewModel(gateway)
    }

    @Test
    fun `test that uiState is hidden before any player state arrives`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(AudioMiniPlayerUiState.Hidden)
        }
    }

    @Test
    fun `test that uiState maps the player state`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(
                AudioControllerState(
                    mediaItemCount = 3,
                    title = "Orange (Live)",
                    artist = "Arcy Drive",
                    isPlaying = true,
                    currentPositionMs = 30_000L,
                    durationMs = 120_000L,
                )
            )
            val state = awaitItem() as AudioMiniPlayerUiState.Data
            assertThat(state.title).isEqualTo("Orange (Live)")
            assertThat(state.artist).isEqualTo("Arcy Drive")
            assertThat(state.isPlaying).isTrue()
            assertThat(state.progress).isEqualTo(0.25f)
        }
    }

    @Test
    fun `test that uiState is hidden when the queue is empty`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(AudioMiniPlayerUiState.Hidden)
            playerStateFlow.emit(AudioControllerState(mediaItemCount = 0, title = "Orange (Live)"))
            expectNoEvents()
        }
    }

    @Test
    fun `test that uiState is hidden when the session ended`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(
                AudioControllerState(
                    mediaItemCount = 3,
                    title = "Orange (Live)",
                    isSessionEnded = true,
                )
            )
            expectNoEvents()
        }
    }

    @Test
    fun `test that progress is zero when the duration is unknown`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(
                AudioControllerState(
                    mediaItemCount = 1,
                    currentPositionMs = 5_000L,
                    durationMs = 0L,
                )
            )
            assertThat((awaitItem() as AudioMiniPlayerUiState.Data).progress).isEqualTo(0f)
        }
    }

    @Test
    fun `test that progress is clamped to one when the position is reported beyond the duration`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(
                AudioControllerState(
                    mediaItemCount = 1,
                    title = "Orange (Live)",
                    currentPositionMs = 200_000L,
                    durationMs = 120_000L,
                )
            )
            assertThat((awaitItem() as AudioMiniPlayerUiState.Data).progress).isEqualTo(1f)
        }
    }

    @Test
    fun `test that uiState falls back to hidden when the player state flow fails`() = runTest {
        whenever(gateway.playerState).thenReturn(
            flow {
                emit(AudioControllerState(mediaItemCount = 1, title = "Orange (Live)"))
                // Without a suspension point the failure would land in the same dispatch as the
                // emission, and the conflating StateFlow would only ever show the fallback.
                delay(100)
                throw IllegalStateException("controller disconnected")
            }
        )
        underTest = AudioMiniPlayerViewModel(gateway)

        underTest.uiState.test {
            var visible = awaitItem()
            while (visible !is AudioMiniPlayerUiState.Data) visible = awaitItem()
            assertThat(visible.title).isEqualTo("Orange (Live)")

            assertThat(awaitItem()).isEqualTo(AudioMiniPlayerUiState.Hidden)
        }
    }

    @Test
    fun `test that uiState reports loading while buffering`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(AudioControllerState(mediaItemCount = 1, isBuffering = true))
            assertThat((awaitItem() as AudioMiniPlayerUiState.Data).isLoading).isTrue()
        }
    }

    @Test
    fun `test that onPlayPauseClicked pauses when playing`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(AudioControllerState(mediaItemCount = 1, isPlaying = true))
            awaitItem()
            underTest.onPlayPauseClicked()
        }
        verify(gateway).pause()
        verify(gateway, never()).play()
    }

    @Test
    fun `test that onPlayPauseClicked plays when paused`() = runTest {
        underTest.uiState.test {
            awaitItem()
            playerStateFlow.emit(AudioControllerState(mediaItemCount = 1, isPlaying = false))
            awaitItem()
            underTest.onPlayPauseClicked()
        }
        verify(gateway).play()
        verify(gateway, never()).pause()
    }

    @Test
    fun `test that the media controller is released when the ViewModel is cleared`() = runTest {
        ViewModelStore().apply { put("mini_player", underTest) }.clear()

        verify(gateway).release()
    }
}
