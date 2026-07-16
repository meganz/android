package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.media3.common.Player
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerGateway
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.mediaplayer.model.AudioControllerState
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.URL_LOCAL_FILE_PATH
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_LINK_ADAPTER
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

    @Test
    fun `test that startPlayback sets nodeSourceType to OFFLINE when adapter type is OFFLINE_ADAPTER`() =
        runTest {
            val intent = mockIntent(adapterType = OFFLINE_ADAPTER)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.OFFLINE)
            }
        }

    @Test
    fun `test that startPlayback sets nodeSourceType to FOLDER_LINK when adapter type is FOLDER_LINK_ADAPTER`() =
        runTest {
            val intent = mockIntent(adapterType = FOLDER_LINK_ADAPTER)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.FOLDER_LINK)
            }
        }

    @Test
    fun `test that startPlayback sets nodeSourceType to FOLDER_LINK when adapter type is FROM_ALBUM_SHARING`() =
        runTest {
            val intent = mockIntent(adapterType = FROM_ALBUM_SHARING)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.FOLDER_LINK)
            }
        }

    @Test
    fun `test that startPlayback sets nodeSourceType to CHAT when adapter type is FROM_CHAT`() =
        runTest {
            val intent = mockIntent(adapterType = FROM_CHAT)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.CHAT)
            }
        }

    @Test
    fun `test that startPlayback sets nodeSourceType to FILE_LINK when adapter type is FILE_LINK_ADAPTER`() =
        runTest {
            val intent = mockIntent(adapterType = FILE_LINK_ADAPTER)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.FILE_LINK)
            }
        }

    @Test
    fun `test that startPlayback sets nodeSourceType to VIDEO_PLAYER_DEFAULT when adapter type is not a known type`() =
        runTest {
            val intent = mockIntent(adapterType = 9999)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.VIDEO_PLAYER_DEFAULT)
            }
        }

    @Test
    fun `test that startPlayback sets fileLinkUrl in uiState from URL_FILE_LINK intent extra`() =
        runTest {
            val intent = mockIntent(fileLinkUrl = "https://mega.nz/file/abc123")
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.fileLinkUrl).isEqualTo("https://mega.nz/file/abc123")
            }
        }

    @Test
    fun `test that startPlayback sets chatId in uiState when INTENT_EXTRA_KEY_CHAT_ID is a valid handle`() =
        runTest {
            val intent = mockIntent(chatId = 12345L)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.chatId).isEqualTo(12345L)
            }
        }

    @Test
    fun `test that startPlayback sets chatId to null in uiState when INTENT_EXTRA_KEY_CHAT_ID is INVALID_HANDLE`() =
        runTest {
            // Use OFFLINE_ADAPTER to ensure the state changes and a new emission is triggered.
            val intent = mockIntent(adapterType = OFFLINE_ADAPTER, chatId = -1L)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(AudioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.chatId).isNull()
            }
        }

    private fun mockIntent(
        adapterType: Int = -1,
        fileLinkUrl: String? = null,
        localFilePath: String? = null,
        chatId: Long = -1L,
        msgId: Long = -1L,
    ): Intent = mock<Intent>().apply {
        whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(false)
        whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(adapterType)
        whenever(getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, -1L)).thenReturn(chatId)
        whenever(getLongExtra(INTENT_EXTRA_KEY_MSG_ID, -1L)).thenReturn(msgId)
        whenever(getStringExtra(URL_FILE_LINK)).thenReturn(fileLinkUrl)
        whenever(getStringExtra(URL_LOCAL_FILE_PATH)).thenReturn(localFilePath)
    }
}
