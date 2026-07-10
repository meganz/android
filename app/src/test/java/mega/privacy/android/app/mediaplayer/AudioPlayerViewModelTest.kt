package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.media3.common.Player
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerGateway
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.mediaplayer.model.AudioControllerState
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.AfterEach
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
class AudioPlayerViewModelTest {

    private lateinit var underTest: AudioPlayerViewModel

    // Recreated in setUp() so the replay cache from a previous test never leaks into the next
    // ViewModel instance and causes handleSideEffects to fire with stale state.
    private lateinit var gatewayPlayerState: MutableSharedFlow<AudioControllerState>
    private val gateway = mock<AudioMediaControllerGateway>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val setAudioShuffleEnabledUseCase = mock<SetAudioShuffleEnabledUseCase>()
    private val setAudioRepeatModeUseCase = mock<SetAudioRepeatModeUseCase>()
    private val repeatToggleModeByExoPlayerMapper = mock<RepeatToggleModeByExoPlayerMapper>()

    @BeforeEach
    fun setUp() {
        // Analytics must be initialised before any test that triggers handleSideEffects, because
        // Analytics.tracker throws IllegalStateException when uninitialised and that propagates
        // out of the collect lambda, killing the observePlayerState coroutine.
        Analytics.initialise(mock<AnalyticsTracker>())
        // replay = 1: when gatewayPlayerState.emit() is called inside uiState.test{} the
        // observePlayerState coroutine may not have subscribed yet (lazy scheduler). The replay
        // cache ensures the value is delivered once the coroutine does subscribe.
        gatewayPlayerState = MutableSharedFlow(replay = 1)
        whenever(gateway.playerState).thenReturn(gatewayPlayerState)
        underTest = AudioPlayerViewModel(
            gateway = gateway,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            setAudioShuffleEnabledUseCase = setAudioShuffleEnabledUseCase,
            setAudioRepeatModeUseCase = setAudioRepeatModeUseCase,
            repeatToggleModeByExoPlayerMapper = repeatToggleModeByExoPlayerMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        Analytics.initialise(null as AnalyticsTracker?)
        reset(
            gateway,
            getNodeByHandleUseCase,
            setAudioShuffleEnabledUseCase,
            setAudioRepeatModeUseCase,
            repeatToggleModeByExoPlayerMapper,
        )
    }

    @Test
    fun `test that uiState emits Loading before MediaController connects`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(AudioPlayerUiState.Loading)
    }

    @Test
    fun `test that uiState emits Data when gateway emits controller state`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading

            gatewayPlayerState.emit(AudioControllerState(isPlaying = true, durationMs = 60_000L))

            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.isPlaying).isTrue()
            assertThat(state.duration).isEqualTo(60_000L)
        }
    }

    @Test
    fun `test that togglePlayPause calls pause when current state is playing`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(isPlaying = true))
            awaitItem() // Data
            underTest.togglePlayPause()
            verify(gateway).pause()
        }
    }

    @Test
    fun `test that togglePlayPause calls play when current state is not playing`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(isPlaying = false))
            awaitItem() // Data
            underTest.togglePlayPause()
            verify(gateway).play()
        }
    }

    @Test
    fun `test that togglePlayPause does nothing before first controller state is received`() =
        runTest {
            underTest.togglePlayPause()
            verify(gateway, never()).play()
            verify(gateway, never()).pause()
        }

    @Test
    fun `test that seekTo forwards position to gateway`() = runTest {
        underTest.seekTo(12_345L)
        verify(gateway).seekTo(12_345L)
    }

    @Test
    fun `test that skipToNext calls skipToNext on gateway`() = runTest {
        underTest.skipToNext()
        verify(gateway).skipToNext()
    }

    @Test
    fun `test that skipToPrevious calls skipToPrevious on gateway`() = runTest {
        underTest.skipToPrevious()
        verify(gateway).skipToPrevious()
    }

    @Test
    fun `test that toggleShuffle enables shuffle when current state has shuffle disabled`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState(shuffleEnabled = false))
                awaitItem() // Data
                underTest.toggleShuffle()
                verify(gateway).setShuffleEnabled(true)
            }
        }

    @Test
    fun `test that toggleShuffle disables shuffle when current state has shuffle enabled`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState(shuffleEnabled = true))
                awaitItem() // Data
                underTest.toggleShuffle()
                verify(gateway).setShuffleEnabled(false)
            }
        }

    @Test
    fun `test that cycleRepeatMode sets ALL when current mode is OFF`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
            awaitItem() // Data
            underTest.cycleRepeatMode()
            verify(gateway).setRepeatMode(Player.REPEAT_MODE_ALL)
        }
    }

    @Test
    fun `test that cycleRepeatMode sets ONE when current mode is ALL`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_ALL))
            awaitItem() // Data
            underTest.cycleRepeatMode()
            verify(gateway).setRepeatMode(Player.REPEAT_MODE_ONE)
        }
    }

    @Test
    fun `test that cycleRepeatMode sets OFF when current mode is ONE`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_ONE))
            awaitItem() // Data
            underTest.cycleRepeatMode()
            verify(gateway).setRepeatMode(Player.REPEAT_MODE_OFF)
        }
    }

    @Test
    fun `test that setCurrentIntent updates adapter type in uiState`() = runTest {
        val intent = mock<Intent>().apply {
            whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(false)
            whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(42)
        }

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState())
            awaitItem() // Data (initial from gateway)
            underTest.startPlayback(intent)
            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentAdapterType).isEqualTo(42)
        }
    }

    @Test
    fun `test that onMediaItemTransition updates handle and thumbnail in uiState`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState())
            awaitItem() // Data (initial)

            gatewayPlayerState.emit(AudioControllerState(currentMediaItemId = "123456"))

            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentPlayingHandle).isEqualTo(123456L)
            assertThat(state.thumbnailData).isNotNull()
        }
    }

    @Test
    fun `test that uiState emits node name after media item transition`() = runTest {
        val node = mock<FileNode>()
        whenever(node.name).thenReturn("track.mp3")
        whenever(getNodeByHandleUseCase(123456L)).thenReturn(node)

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState())
            awaitItem() // Data (initial, no mediaItemId change)

            gatewayPlayerState.emit(AudioControllerState(currentMediaItemId = "123456"))
            // fetchNodeName runs eagerly on UnconfinedTestDispatcher before mapToUiState, so
            // both playerState updates are conflated by StateFlow into a single emission
            // that carries both the new handle and the resolved node name.
            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentPlayingHandle).isEqualTo(123456L)
            assertThat(state.currentPlayingItemName).isEqualTo("track.mp3")
        }
    }

    @Test
    fun `test that setAudioShuffleEnabledUseCase is called when shuffle mode changes`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(shuffleEnabled = false))
            awaitItem() // Data (initial)

            gatewayPlayerState.emit(AudioControllerState(shuffleEnabled = true))
            awaitItem() // Data with new shuffle
        }

        verify(setAudioShuffleEnabledUseCase).invoke(true)
    }

    @Test
    fun `test that setAudioRepeatModeUseCase is called when repeat mode changes`() = runTest {
        whenever(repeatToggleModeByExoPlayerMapper(Player.REPEAT_MODE_ALL))
            .thenReturn(RepeatToggleMode.REPEAT_ALL)

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
            awaitItem() // Data (initial)

            gatewayPlayerState.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_ALL))
            awaitItem() // Data with new repeat mode
        }

        verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
    }

    @Test
    fun `test that startPlayback calls startService on gateway when rebuildPlaylist is true`() =
        runTest {
            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }
            underTest.startPlayback(intent)
            verify(gateway).startService(intent)
        }

    @Test
    fun `test that startPlayback does not call startService when rebuildPlaylist is false`() =
        runTest {
            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(false)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }
            underTest.startPlayback(intent)
            verify(gateway, never()).startService(intent)
        }
}
