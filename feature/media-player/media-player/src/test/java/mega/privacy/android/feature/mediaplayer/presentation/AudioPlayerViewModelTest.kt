package mega.privacy.android.feature.mediaplayer.presentation

import android.content.Intent
import androidx.media3.common.Player
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.DeleteAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerError
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerErrorEventQueue
import mega.privacy.android.feature.mediaplayer.data.AudioResumeEvent
import mega.privacy.android.feature.mediaplayer.data.AudioResumeEventQueue
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.feature.mediaplayer.data.model.AudioControllerState
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import mega.privacy.android.feature.mediaplayer.presentation.model.SleepTimerOption
import mega.privacy.android.feature.mediaplayer.presentation.model.SleepTimerState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayerViewModelTest {

    private lateinit var underTest: AudioPlayerViewModel

    // Recreated in setUp() so the replay cache from a previous test never leaks into the next
    // ViewModel instance and causes handleSideEffects to fire with stale state.
    private lateinit var gatewayPlayerState: MutableSharedFlow<AudioControllerState>
    private lateinit var nodeUpdates: MutableSharedFlow<NodeUpdate>
    private val gateway = mock<AudioMediaControllerGateway>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()
    private val setAudioShuffleEnabledUseCase = mock<SetAudioShuffleEnabledUseCase>()
    private val setAudioRepeatModeUseCase = mock<SetAudioRepeatModeUseCase>()
    private val deleteAudioPlaybackInfoUseCase = mock<DeleteAudioPlaybackInfoUseCase>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()

    // The ViewModel's wall clock follows the scheduler's virtual time (so armed timers always run
    // down and runTest can finish) plus this offset, which sleep-timer tests bump to simulate a
    // tick that wakes up late.
    private var fakeClockOffsetMillis = 0L

    // Real instance: a stateless mapper, so the tests exercise the real repeat-mode cycle.
    private val repeatToggleModeByExoPlayerMapper = RepeatToggleModeByExoPlayerMapper()

    // Real instance: a plain in-memory holder, recreated per test so a pending event never
    // leaks into the next ViewModel.
    private lateinit var audioResumeEventQueue: AudioResumeEventQueue

    // Real instance for the same reason as audioResumeEventQueue.
    private lateinit var audioPlayerErrorEventQueue: AudioPlayerErrorEventQueue

    // Real instance: a stateless formatter, so the tests assert the string the user actually sees.
    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

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
        nodeUpdates = MutableSharedFlow()
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdates)
        audioResumeEventQueue = AudioResumeEventQueue()
        audioPlayerErrorEventQueue = AudioPlayerErrorEventQueue()
        fakeClockOffsetMillis = 0L
        whenever(getCurrentTimeInMillisUseCase()).thenAnswer {
            mainDispatcherExtension.testDispatcher.scheduler.currentTime + fakeClockOffsetMillis
        }
        underTest = AudioPlayerViewModel(
            gateway = gateway,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            setAudioShuffleEnabledUseCase = setAudioShuffleEnabledUseCase,
            setAudioRepeatModeUseCase = setAudioRepeatModeUseCase,
            deleteAudioPlaybackInfoUseCase = deleteAudioPlaybackInfoUseCase,
            repeatToggleModeByExoPlayerMapper = repeatToggleModeByExoPlayerMapper,
            audioResumeEventQueue = audioResumeEventQueue,
            audioPlayerErrorEventQueue = audioPlayerErrorEventQueue,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
    }

    /**
     * Advances virtual time by [elapsed] and lets the scheduler run the tick due at that moment
     * (advanceTimeBy is exclusive of its end time, hence the extra millisecond).
     */
    private fun TestScope.advanceClock(elapsed: Duration) {
        advanceTimeBy(elapsed + 1.milliseconds)
    }

    @AfterEach
    fun tearDown() {
        Analytics.initialise(null as AnalyticsTracker?)
        reset(
            gateway,
            getNodeByHandleUseCase,
            monitorNodeUpdatesUseCase,
            isParticipatingInChatCallUseCase,
            setAudioShuffleEnabledUseCase,
            setAudioRepeatModeUseCase,
            deleteAudioPlaybackInfoUseCase,
            getCurrentTimeInMillisUseCase,
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

            gatewayPlayerState.emit(audioControllerState(isPlaying = true, durationMs = 60_000L))

            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.isPlaying).isTrue()
            assertThat(state.duration).isEqualTo(60_000L)
        }
    }

    @Test
    fun `test that uiState Data carries hasNext from the controller state`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading

            gatewayPlayerState.emit(audioControllerState(hasNext = true))
            assertThat((awaitItem() as AudioPlayerUiState.Data).hasNext).isTrue()

            gatewayPlayerState.emit(audioControllerState(hasNext = false))
            assertThat((awaitItem() as AudioPlayerUiState.Data).hasNext).isFalse()
        }
    }

    @Test
    fun `test that togglePlayPause calls pause when current state is playing`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(isPlaying = true))
            awaitItem() // Data
            underTest.togglePlayPause()
            verify(gateway).pause()
        }
    }

    @Test
    fun `test that togglePlayPause calls play when current state is not playing`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(isPlaying = false))
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
    fun `test that seekForward15 calls seekTo with current position plus 15 seconds`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(currentPositionMs = 30_000L))
            awaitItem() // Data
            underTest.seekForward15()
            verify(gateway).seekTo(45_000L)
        }
    }

    @Test
    fun `test that seekBackward15 calls seekTo with current position minus 15 seconds`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(currentPositionMs = 30_000L))
            awaitItem() // Data
            underTest.seekBackward15()
            verify(gateway).seekTo(15_000L)
        }
    }

    @Test
    fun `test that seekBackward15 clamps to zero when current position is less than 15 seconds`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(currentPositionMs = 5_000L))
                awaitItem() // Data
                underTest.seekBackward15()
                verify(gateway).seekTo(0L)
            }
        }

    @Test
    fun `test that toggleShuffle enables shuffle when current state has shuffle disabled`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(shuffleEnabled = false))
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
                gatewayPlayerState.emit(audioControllerState(shuffleEnabled = true))
                awaitItem() // Data
                underTest.toggleShuffle()
                verify(gateway).setShuffleEnabled(false)
            }
        }

    @Test
    fun `test that setAudioShuffleEnabledUseCase is called when shuffle mode changes`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(shuffleEnabled = false))
            awaitItem() // Data (initial)

            gatewayPlayerState.emit(audioControllerState(shuffleEnabled = true))
            awaitItem() // Data with new shuffle
        }

        verify(setAudioShuffleEnabledUseCase).invoke(true)
    }

    @Test
    fun `test that cycleRepeatMode persists REPEAT_ALL when current mode is OFF`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
            awaitItem() // Data
            underTest.cycleRepeatMode()
        }

        verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
    }

    @Test
    fun `test that cycleRepeatMode persists REPEAT_ONE when current mode is ALL`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ALL))
            awaitItem() // Data
            underTest.cycleRepeatMode()
        }

        verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ONE.ordinal)
    }

    @Test
    fun `test that cycleRepeatMode persists REPEAT_NONE when current mode is ONE`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ONE))
            awaitItem() // Data
            underTest.cycleRepeatMode()
        }

        verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_NONE.ordinal)
    }

    @Test
    fun `test that cycleRepeatMode does not persist a repeat mode before the player state arrives`() =
        runTest {
            underTest.cycleRepeatMode()
            advanceUntilIdle()

            verify(setAudioRepeatModeUseCase, never()).invoke(any())
        }

    @Test
    fun `test that consecutive cycleRepeatMode calls advance the mode before the player reports it`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data

                underTest.cycleRepeatMode()
                underTest.cycleRepeatMode()
            }
            advanceUntilIdle()

            inOrder(setAudioRepeatModeUseCase) {
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ONE.ordinal)
            }
        }

    @Test
    fun `test that four consecutive cycleRepeatMode calls write the whole cycle in order`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data

                underTest.cycleRepeatMode()
                underTest.cycleRepeatMode()
                underTest.cycleRepeatMode()
                gatewayPlayerState.emit(
                    audioControllerState(
                        repeatMode = Player.REPEAT_MODE_OFF,
                        currentPositionMs = 1_000L,
                    )
                )
                awaitItem() // Data from a position tick, still reporting the old mode

                underTest.cycleRepeatMode()
            }
            advanceUntilIdle()

            inOrder(setAudioRepeatModeUseCase) {
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ONE.ordinal)
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_NONE.ordinal)
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
            }
        }

    @Test
    fun `test that cycleRepeatMode resumes from the player state once it reports the pending mode`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data

                underTest.cycleRepeatMode()
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ALL))
                awaitItem() // Data with the applied mode

                underTest.cycleRepeatMode()
            }
            advanceUntilIdle()

            verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ONE.ordinal)
        }

    @Test
    fun `test that a repeat mode report clears a stale pending mode once no write is in flight`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data

                underTest.cycleRepeatMode() // pending = ALL
                advanceUntilIdle() // the write settles without the player ever reporting ALL

                // Another writer (e.g. the queue screen's continuous-playback toggle) settled
                // the player on a different mode.
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ONE))
                awaitItem() // Data with the externally applied mode

                underTest.cycleRepeatMode()
            }
            advanceUntilIdle()

            // The second tap must cycle from the reported ONE, not from the stale pending ALL.
            verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_NONE.ordinal)
        }

    @Test
    fun `test that a repeat mode report keeps the pending mode while a write is in flight`() =
        runTest {
            val writeGate = CompletableDeferred<Unit>()
            whenever(setAudioRepeatModeUseCase(any())).doSuspendableAnswer { writeGate.await() }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data

                underTest.cycleRepeatMode() // pending = ALL, write suspended at the gate

                // A report of a mode the tap never asked for arrives while the write is still
                // in flight — it may simply be an older state, so pending must survive it.
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ONE))
                awaitItem() // Data with the reported mode

                underTest.cycleRepeatMode()
            }
            writeGate.complete(Unit)
            advanceUntilIdle()

            inOrder(setAudioRepeatModeUseCase) {
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ALL.ordinal)
                verify(setAudioRepeatModeUseCase).invoke(RepeatToggleMode.REPEAT_ONE.ordinal)
            }
        }

    @Test
    fun `test that setAudioRepeatModeUseCase is not called when the player reports a repeat mode change`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_OFF))
                awaitItem() // Data (initial)

                gatewayPlayerState.emit(audioControllerState(repeatMode = Player.REPEAT_MODE_ALL))
                awaitItem() // Data with new repeat mode
            }

            verify(setAudioRepeatModeUseCase, never()).invoke(any())
        }

    @Test
    fun `test that setCurrentIntent updates adapter type in uiState`() = runTest {
        val intent = mock<Intent>().apply {
            whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(false)
            whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(42)
        }

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState())
            awaitItem() // Data (initial from gateway)
            underTest.startPlayback(intent)
            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentAdapterType).isEqualTo(42)
        }
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
    fun `test that startPlayback clears a pending playback error when rebuilding the playlist`() =
        runTest {
            underTest.playbackError.test {
                assertThat(awaitItem()).isNull()
                audioPlayerErrorEventQueue.emit(AudioPlayerError.RetryExhausted(isNetworkAvailable = true))
                assertThat(awaitItem()).isNotNull()

                val intent = mock<Intent>().apply {
                    whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true))
                        .thenReturn(true)
                    whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
                }
                underTest.startPlayback(intent)

                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `test that startPlayback keeps a pending playback error when not rebuilding the playlist`() =
        runTest {
            underTest.playbackError.test {
                assertThat(awaitItem()).isNull()
                audioPlayerErrorEventQueue.emit(AudioPlayerError.NodeBlocked)
                assertThat(awaitItem()).isEqualTo(AudioPlayerError.NodeBlocked)

                val intent = mock<Intent>().apply {
                    whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true))
                        .thenReturn(false)
                    whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
                }
                underTest.startPlayback(intent)

                expectNoEvents()
            }
        }

    @Test
    fun `test that hasStartedPlayback turns true once startPlayback receives an intent`() =
        runTest {
            assertThat(underTest.hasStartedPlayback).isFalse()

            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(false)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }
            underTest.startPlayback(intent)

            assertThat(underTest.hasStartedPlayback).isTrue()
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
                awaitItem() // Data (initial)
                underTest.startPlayback(intent)
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.MEDIA_PLAYER_DEFAULT)
            }
        }

    @Test
    fun `test that startPlayback sets fileLinkUrl in uiState from URL_FILE_LINK intent extra`() =
        runTest {
            val intent = mockIntent(fileLinkUrl = "https://mega.nz/file/abc123")
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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
                gatewayPlayerState.emit(audioControllerState())
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

    @Test
    fun `test that onMediaItemTransition updates handle and thumbnail in uiState`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState())
            awaitItem() // Data (initial)

            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "some-uuid",
                    currentMediaItemHandle = 123456L
                )
            )

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
            gatewayPlayerState.emit(audioControllerState())
            awaitItem() // Data (initial, no mediaItemId change)

            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "some-uuid",
                    currentMediaItemHandle = 123456L
                )
            )
            // fetchNodeName runs eagerly on UnconfinedTestDispatcher before mapToUiState, so
            // both playerState updates are conflated by StateFlow into a single emission
            // that carries both the new handle and the resolved node name.
            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentPlayingHandle).isEqualTo(123456L)
            assertThat(state.currentPlayingItemName).isEqualTo("track.mp3")
        }
    }

    @Test
    fun `test that a late node name for a previous track does not overwrite the current track name`() =
        runTest {
            val previousNodeFetch = CompletableDeferred<FileNode>()
            val previousNode = mock<FileNode>()
            whenever(previousNode.name).thenReturn("previous.mp3")
            val currentNode = mock<FileNode>()
            whenever(currentNode.name).thenReturn("current.mp3")
            whenever(getNodeByHandleUseCase(111L)).doSuspendableAnswer { previousNodeFetch.await() }
            whenever(getNodeByHandleUseCase(222L)).thenReturn(currentNode)

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(currentMediaItemId = "a", currentMediaItemHandle = 111L)
                )
                awaitItem() // Data for 111, its name fetch still in flight

                gatewayPlayerState.emit(
                    audioControllerState(currentMediaItemId = "b", currentMediaItemHandle = 222L)
                )
                val current = expectMostRecentItem() as AudioPlayerUiState.Data
                assertThat(current.currentPlayingItemName).isEqualTo("current.mp3")

                // The fetch for the previous track resolves only now.
                previousNodeFetch.complete(previousNode)

                expectNoEvents()
                val latest = underTest.uiState.value as AudioPlayerUiState.Data
                assertThat(latest.currentPlayingItemName).isEqualTo("current.mp3")
            }
        }

    @Test
    fun `test that handleSideEffects fetches node name on first emission when media item handle is set`() =
        runTest {
            val node = mock<FileNode>()
            whenever(node.name).thenReturn("track.mp3")
            whenever(getNodeByHandleUseCase(123456L)).thenReturn(node)

            underTest.uiState.test {
                awaitItem() // Loading

                // First emission already carries a handle — fetchNodeName must fire even
                // though there is no previous state (prev == null path).
                gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123456L))

                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.currentPlayingItemName).isEqualTo("track.mp3")
            }
        }

    @Test
    fun `test that handleSideEffects does not call getNodeByHandleUseCase on first emission when media item handle is null`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading

                gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = null))
                awaitItem() // Data

                verify(getNodeByHandleUseCase, never()).invoke(any(), any())
            }
        }

    @Test
    fun `test that isPodcastMode is false by default`() = runTest {
        assertThat(underTest.isPodcastMode.value).isFalse()
    }

    @Test
    fun `test that isPodcastMode stays false when player duration is below podcast threshold`() =
        runTest {
            underTest.isPodcastMode.test {
                awaitItem() // default false
                gatewayPlayerState.emit(audioControllerState(durationMs = 5 * 60 * 1_000L))
                expectNoEvents()
            }
        }

    @Test
    fun `test that isPodcastMode becomes true when player duration exceeds podcast threshold`() =
        runTest {
            underTest.isPodcastMode.test {
                awaitItem() // default false
                gatewayPlayerState.emit(audioControllerState(durationMs = 15 * 60 * 1_000L))
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that togglePlayerMode sets isPodcastMode to false when in podcast mode`() = runTest {
        underTest.isPodcastMode.test {
            awaitItem() // default false
            gatewayPlayerState.emit(audioControllerState(durationMs = 15 * 60 * 1_000L))
            awaitItem() // podcast auto-detected
            underTest.togglePlayerMode()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that togglePlayerMode sets isPodcastMode to true when in music mode`() = runTest {
        underTest.isPodcastMode.test {
            awaitItem() // default false
            underTest.togglePlayerMode()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that togglePlayerMode override is not overwritten by player state update`() =
        runTest {
            underTest.isPodcastMode.test {
                awaitItem() // default false
                underTest.togglePlayerMode()
                assertThat(awaitItem()).isTrue()
                // Same track, short duration — override must be preserved
                gatewayPlayerState.emit(
                    audioControllerState(durationMs = 5 * 60 * 1_000L, currentMediaItemId = null)
                )
                expectNoEvents()
            }
        }

    @Test
    fun `test that isPodcastMode updates from player state after track changes`() = runTest {
        underTest.isPodcastMode.test {
            awaitItem() // default false
            gatewayPlayerState.emit(
                audioControllerState(currentMediaItemId = "track1", durationMs = 15 * 60 * 1_000L)
            )
            assertThat(awaitItem()).isTrue() // podcast auto-detected
            underTest.togglePlayerMode()
            assertThat(awaitItem()).isFalse()
            // Track change resets override — long duration → auto-detected as podcast
            gatewayPlayerState.emit(
                audioControllerState(currentMediaItemId = "track2", durationMs = 15 * 60 * 1_000L)
            )
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that setPlaybackSpeed delegates to gateway`() = runTest {
        underTest.setPlaybackSpeed(1.5f)
        verify(gateway).setPlaybackSpeed(1.5f)
    }

    @Test
    fun `test that currentPlaybackSpeed in uiState reflects playback speed from player state`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(playbackSpeed = 1.5f))
                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.currentPlaybackSpeed).isEqualTo(1.5f)
            }
        }

    @Test
    fun `test that a failing shuffle persistence does not crash and uiState keeps updating`() =
        runTest {
            whenever(setAudioShuffleEnabledUseCase(true)).doSuspendableAnswer {
                throw IOException("disk full")
            }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(shuffleEnabled = false))
                awaitItem() // Data, shuffle off

                gatewayPlayerState.emit(audioControllerState(shuffleEnabled = true))

                val state = awaitItem() as AudioPlayerUiState.Data
                assertThat(state.shuffleEnabled).isTrue()
                verify(setAudioShuffleEnabledUseCase).invoke(true)
            }
        }

    @Test
    fun `test that sleepTimerState is Inactive initially`() = runTest {
        assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
    }

    @Test
    fun `test that setSleepTimer emits CountingDown immediately when a timed option is selected`() =
        runTest {
            underTest.sleepTimerState.test {
                assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
                underTest.setSleepTimer(SleepTimerOption.Minutes5)
                val state = awaitItem() as SleepTimerState.CountingDown
                assertThat(state.option).isEqualTo(SleepTimerOption.Minutes5)
                assertThat(state.remaining).isEqualTo(SleepTimerOption.Minutes5.duration)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setSleepTimer emits EndOfTrack when EndOfTrack option is selected`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)
            assertThat(awaitItem()).isEqualTo(SleepTimerState.EndOfTrack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that cancelSleepTimer emits Inactive when a timed countdown is active`() = runTest {
        underTest.setSleepTimer(SleepTimerOption.Minutes5)
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isInstanceOf(SleepTimerState.CountingDown::class.java)
            underTest.cancelSleepTimer()
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that cancelSleepTimer emits Inactive when EndOfTrack mode is active`() = runTest {
        underTest.setSleepTimer(SleepTimerOption.EndOfTrack)
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.EndOfTrack)
            underTest.cancelSleepTimer()
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer replaces the active option when called again`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.Minutes60)
            awaitItem() // Minutes60 CountingDown
            underTest.setSleepTimer(SleepTimerOption.Minutes15)
            val state = awaitItem() as SleepTimerState.CountingDown
            assertThat(state.option).isEqualTo(SleepTimerOption.Minutes15)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer replaces EndOfTrack mode with a timed countdown`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)
            assertThat(awaitItem()).isEqualTo(SleepTimerState.EndOfTrack)
            underTest.setSleepTimer(SleepTimerOption.Minutes30)
            val state = awaitItem() as SleepTimerState.CountingDown
            assertThat(state.option).isEqualTo(SleepTimerOption.Minutes30)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer replaces a timed countdown with EndOfTrack mode`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.Minutes30)
            awaitItem() // CountingDown
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)
            assertThat(awaitItem()).isEqualTo(SleepTimerState.EndOfTrack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer decrements remaining after each tick`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.Minutes5)
            awaitItem() // initial CountingDown(300_000)
            advanceClock(1.seconds)
            val afterOneTick = awaitItem() as SleepTimerState.CountingDown
            assertThat(afterOneTick.remaining).isEqualTo(299.seconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer follows the wall clock when a tick wakes up late`() = runTest {
        underTest.sleepTimerState.test {
            assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
            underTest.setSleepTimer(SleepTimerOption.Minutes5)
            awaitItem() // initial CountingDown(300_000)

            // The 1 s tick wakes up 3 s late: the wall clock reads 4 s past the start, so the
            // remaining time must reflect those 4 s, not the single tick that was scheduled.
            fakeClockOffsetMillis += 3_000L
            advanceClock(1.seconds)

            val afterLateTick = awaitItem() as SleepTimerState.CountingDown
            assertThat(afterLateTick.remaining).isEqualTo(296.seconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setSleepTimer never reports more than the chosen duration when the clock jumps back`() =
        runTest {
            underTest.sleepTimerState.test {
                assertThat(awaitItem()).isEqualTo(SleepTimerState.Inactive)
                underTest.setSleepTimer(SleepTimerOption.Minutes5)
                awaitItem() // initial CountingDown(300_000)

                // The wall clock is set back by a minute before the first tick fires. The clamped
                // value equals the initial one, so the StateFlow emits nothing new.
                fakeClockOffsetMillis -= 60_000L
                advanceClock(1.seconds)

                expectNoEvents()
                val afterJump = underTest.sleepTimerState.value as SleepTimerState.CountingDown
                assertThat(afterJump.remaining).isEqualTo(5.minutes)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setSleepTimer pauses gateway and emits Inactive when countdown expires`() =
        runTest {
            underTest.setSleepTimer(SleepTimerOption.Minutes5)
            advanceClock(5.minutes)
            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
            verify(gateway).pause()
        }

    @Test
    fun `test that sleepTimerState becomes Inactive and gateway pauses when the track auto-advances during EndOfTrack mode`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemId = "track-1"))
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

            gatewayPlayerState.emit(
                audioControllerState(currentMediaItemId = "track-2", lastItemTransitionWasAuto = true)
            )

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
            verify(gateway).pause()
        }

    @Test
    fun `test that sleepTimerState becomes Inactive without pausing when the user changes track during EndOfTrack mode`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemId = "track-1"))
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

            // A skip / queue tap / notification-next arrives as a non-AUTO item transition.
            gatewayPlayerState.emit(
                audioControllerState(currentMediaItemId = "track-2", lastItemTransitionWasAuto = false)
            )

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
            verify(gateway, never()).pause()
        }

    @Test
    fun `test that sleepTimerState becomes Inactive when player stops naturally near end of track in EndOfTrack mode`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemId = "track-1"))
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

            val durationMs = 10_000L
            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "track-1",
                    isPlaying = true,
                    durationMs = durationMs,
                    currentPositionMs = 0L,
                )
            )
            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "track-1",
                    isPlaying = false,
                    durationMs = durationMs,
                    currentPositionMs = 9_000L, // within 1500 ms of end
                )
            )

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
        }

    @Test
    fun `test that sleepTimerState remains EndOfTrack when player pauses far from end of track`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemId = "track-1"))
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

            val durationMs = 10_000L
            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "track-1",
                    isPlaying = true,
                    durationMs = durationMs,
                    currentPositionMs = 5_000L,
                )
            )
            gatewayPlayerState.emit(
                audioControllerState(
                    currentMediaItemId = "track-1",
                    isPlaying = false,
                    durationMs = durationMs,
                    currentPositionMs = 5_000L, // more than 1500 ms from end
                )
            )

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.EndOfTrack)
        }

    @Test
    fun `test that sleepTimerState becomes Inactive when player becomes idle while countdown is active`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 1, isIdle = false))
            underTest.setSleepTimer(SleepTimerOption.Minutes5)
            assertThat(underTest.sleepTimerState.value).isInstanceOf(SleepTimerState.CountingDown::class.java)

            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 1, isIdle = true))

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
        }

    @Test
    fun `test that sleepTimerState becomes Inactive when player becomes idle while EndOfTrack mode is active`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 1, isIdle = false))
            underTest.setSleepTimer(SleepTimerOption.EndOfTrack)
            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.EndOfTrack)

            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 1, isIdle = true))

            assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
        }


    @Test
    fun `test that resumedFromPosition emits the formatted position when the service queues a resume event`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))
            val event = AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 945_000L)

            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()

                audioResumeEventQueue.emit(event)

                assertThat(awaitItem()).isEqualTo("15:45")
            }
        }

    @Test
    fun `test that resumedFromPosition includes hours when the resume position is past an hour`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))
            val event = AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 3_945_000L)

            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()

                audioResumeEventQueue.emit(event)

                assertThat(awaitItem()).isEqualTo("1:05:45")
            }
        }

    @Test
    fun `test that resumedFromPosition stays null when the resume event is for another item`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 456L))
            val event = AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 945_000L)

            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()

                audioResumeEventQueue.emit(event)

                expectNoEvents()
            }
        }

    @Test
    fun `test that resumedFromPosition emits once the player reports the item the event belongs to`() =
        runTest {
            val event = AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 945_000L)

            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()

                // The service emits at play(), which can land before Media3 reports the new item.
                gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 456L))
                audioResumeEventQueue.emit(event)
                expectNoEvents()

                gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))

                assertThat(awaitItem()).isEqualTo("15:45")
            }
        }

    @Test
    fun `test that startPlayback clears a stale resume announcement`() = runTest {
        gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))

        underTest.resumedFromPosition.test {
            assertThat(awaitItem()).isNull()

            // Latched while no screen was attached to consume it.
            audioResumeEventQueue.emit(AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 945_000L))
            assertThat(awaitItem()).isEqualTo("15:45")

            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }
            underTest.startPlayback(intent)

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that onResumeEventConsumed clears the pending resume event`() = runTest {
        gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))
        val event = AudioResumeEvent(mediaHandle = 123L, resumePositionMs = 945_000L)

        underTest.resumedFromPosition.test {
            assertThat(awaitItem()).isNull()
            audioResumeEventQueue.emit(event)
            assertThat(awaitItem()).isEqualTo("15:45")

            underTest.onResumeEventConsumed()

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that restartFromBeginning seeks to start and discards the resumed track's saved position`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123456L))
            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()
                audioResumeEventQueue.emit(
                    AudioResumeEvent(mediaHandle = 123456L, resumePositionMs = 60_000L)
                )
                assertThat(awaitItem()).isNotNull() // announcement armed for 123456

                underTest.restartFromBeginning()

                verify(gateway).seekTo(0L)
                verify(deleteAudioPlaybackInfoUseCase).invoke(123456L)
            }
        }

    @Test
    fun `test that restartFromBeginning still restarts the resumed track after the announcement was consumed`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123456L))
            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()
                audioResumeEventQueue.emit(
                    AudioResumeEvent(mediaHandle = 123456L, resumePositionMs = 60_000L)
                )
                assertThat(awaitItem()).isNotNull()

                // The screen consumes the announcement as soon as the snackbar is queued; the
                // Restart action is tapped afterwards, while the snackbar is still visible.
                underTest.onResumeEventConsumed()
                assertThat(awaitItem()).isNull()

                underTest.restartFromBeginning()

                verify(gateway).seekTo(0L)
                verify(deleteAudioPlaybackInfoUseCase).invoke(123456L)
            }
        }

    @Test
    fun `test that restartFromBeginning does nothing when no resume announcement is pending`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123456L))

            underTest.restartFromBeginning()

            verify(gateway, never()).seekTo(any())
            verify(deleteAudioPlaybackInfoUseCase, never()).invoke(any())
        }

    @Test
    fun `test that restartFromBeginning does nothing when the resumed track has auto-advanced`() =
        runTest {
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123456L))
            underTest.resumedFromPosition.test {
                assertThat(awaitItem()).isNull()
                audioResumeEventQueue.emit(
                    AudioResumeEvent(mediaHandle = 123456L, resumePositionMs = 60_000L)
                )
                assertThat(awaitItem()).isNotNull()

                // Auto-advanced to another track while the snackbar was still showing.
                gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 999L))

                underTest.restartFromBeginning()

                verify(gateway, never()).seekTo(any())
                verify(deleteAudioPlaybackInfoUseCase, never()).invoke(any())
            }
        }

    @Test
    fun `test that uiState stays Loading when controller state has no media items`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(AudioPlayerUiState.Loading)

            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 0, isIdle = true))

            expectNoEvents()
        }
    }

    @Test
    fun `test that playbackError emits when the service parks a terminal error`() = runTest {
        underTest.playbackError.test {
            assertThat(awaitItem()).isNull()

            audioPlayerErrorEventQueue.emit(AudioPlayerError.RetryExhausted(isNetworkAvailable = true))

            assertThat(awaitItem())
                .isEqualTo(AudioPlayerError.RetryExhausted(isNetworkAvailable = true))
        }
    }

    @Test
    fun `test that onPlaybackErrorConsumed clears playbackError`() = runTest {
        underTest.playbackError.test {
            assertThat(awaitItem()).isNull()
            audioPlayerErrorEventQueue.emit(AudioPlayerError.NodeBlocked)
            assertThat(awaitItem()).isEqualTo(AudioPlayerError.NodeBlocked)

            underTest.onPlaybackErrorConsumed()

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that a rename of the playing node refreshes currentPlayingItemName`() = runTest {
        val originalNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("original.mp3")
        }
        val renamedNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("renamed.mp3")
        }
        whenever(getNodeByHandleUseCase(123L)).thenReturn(originalNode, renamedNode)

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))
            awaitItem() // Data with the original name

            nodeUpdates.emit(
                NodeUpdate(mapOf<Node, List<NodeChanges>>(renamedNode to listOf(NodeChanges.Name)))
            )

            val state = awaitItem() as AudioPlayerUiState.Data
            assertThat(state.currentPlayingItemName).isEqualTo("renamed.mp3")
        }
    }

    @Test
    fun `test that removal of the playing node skips to the next item`() = runTest {
        val removedNode = mock<FileNode> { on { id }.thenReturn(NodeId(123L)) }

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(
                audioControllerState(currentMediaItemHandle = 123L, mediaItemCount = 2, hasNext = true)
            )
            awaitItem() // Data

            nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(removedNode to listOf(NodeChanges.Remove))))

            verify(gateway).skipToNext()
        }
    }

    @Test
    fun `test that removal of the playing node stops playback when repeat all would only wrap onto the removed item`() =
        runTest {
            val removedNode = mock<FileNode> { on { id }.thenReturn(NodeId(123L)) }

            underTest.uiState.test {
                awaitItem() // Loading
                // Under repeat-all a single-item queue still reports hasNext: the "next" item is
                // the removed one itself, so playback must stop instead of skipping.
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemHandle = 123L,
                        mediaItemCount = 1,
                        repeatMode = Player.REPEAT_MODE_ALL,
                        hasNext = true,
                    )
                )
                awaitItem() // Data

                nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(removedNode to listOf(NodeChanges.Remove))))

                verify(gateway).stop()
                verify(gateway, never()).skipToNext()
            }
        }

    @Test
    fun `test that removal of the playing node stops playback when it is the only queue item`() =
        runTest {
            val removedNode = mock<FileNode> { on { id }.thenReturn(NodeId(123L)) }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(currentMediaItemHandle = 123L, mediaItemCount = 1)
                )
                awaitItem() // Data

                nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(removedNode to listOf(NodeChanges.Remove))))

                verify(gateway).stop()
                verify(gateway, never()).skipToNext()
            }
        }

    @Test
    fun `test that removal of the playing node stops playback when it is the last item without repeat all`() =
        runTest {
            val removedNode = mock<FileNode> { on { id }.thenReturn(NodeId(123L)) }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemHandle = 123L,
                        mediaItemCount = 2,
                        currentQueueIndex = 1,
                    )
                )
                awaitItem() // Data

                nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(removedNode to listOf(NodeChanges.Remove))))

                verify(gateway).stop()
                verify(gateway, never()).skipToNext()
            }
        }

    @Test
    fun `test that removal of the playing node skips when repeat all can wrap the queue`() =
        runTest {
            val removedNode = mock<FileNode> { on { id }.thenReturn(NodeId(123L)) }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemHandle = 123L,
                        mediaItemCount = 2,
                        currentQueueIndex = 1,
                        repeatMode = Player.REPEAT_MODE_ALL,
                        hasNext = true,
                    )
                )
                awaitItem() // Data

                nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(removedNode to listOf(NodeChanges.Remove))))

                verify(gateway).skipToNext()
                verify(gateway, never()).stop()
            }
        }

    @Test
    fun `test that node updates for other nodes are ignored`() = runTest {
        val otherNode = mock<FileNode> { on { id }.thenReturn(NodeId(999L)) }

        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(currentMediaItemHandle = 123L))
            awaitItem() // Data

            nodeUpdates.emit(NodeUpdate(mapOf<Node, List<NodeChanges>>(otherNode to listOf(NodeChanges.Remove))))

            verify(gateway, never()).skipToNext()
        }
    }

    @Test
    fun `test that EndOfTrack timer pauses playback when a repeat-one loop wraps the position`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = true,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 59_500L,
                    )
                )
                awaitItem() // Data near the end of the track
                underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

                // The loop wraps: same item, still playing, position jumps back to the start.
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = true,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 500L,
                    )
                )
                awaitItem() // Data after the wrap

                verify(gateway).pause()
                assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.Inactive)
            }
        }

    @Test
    fun `test that EndOfTrack timer survives an in-playback backwards seek from the end`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = true,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 59_500L,
                    )
                )
                awaitItem() // Data near the end of the track
                underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

                // A seek back to mid-track while still playing lands far from the start,
                // which a genuine repeat-one wrap never does.
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = true,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 30_000L,
                    )
                )
                awaitItem()

                verify(gateway, never()).pause()
                assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.EndOfTrack)
            }
        }

    @Test
    fun `test that EndOfTrack timer survives an ordinary backwards seek while paused`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = true,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 59_500L,
                    )
                )
                awaitItem() // Data near the end of the track
                underTest.setSleepTimer(SleepTimerOption.EndOfTrack)

                // Paused before the jump back — not a seamless loop, must not trigger the timer.
                gatewayPlayerState.emit(
                    audioControllerState(
                        currentMediaItemId = "track-1",
                        isPlaying = false,
                        repeatMode = Player.REPEAT_MODE_ONE,
                        durationMs = 60_000L,
                        currentPositionMs = 500L,
                    )
                )
                awaitItem()

                verify(gateway, never()).pause()
                assertThat(underTest.sleepTimerState.value).isEqualTo(SleepTimerState.EndOfTrack)
            }
        }

    @Test
    fun `test that togglePlayPause is refused when a call is ongoing and player is paused`() =
        runTest {
            whenever(isParticipatingInChatCallUseCase()).thenReturn(true)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(isPlaying = false))
                awaitItem() // Data

                underTest.togglePlayPause()

                verify(gateway, never()).play()
                assertThat(underTest.controlBlockedByCall.value).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that togglePlayPause still pauses when a call is ongoing and player is playing`() =
        runTest {
            whenever(isParticipatingInChatCallUseCase()).thenReturn(true)
            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(isPlaying = true))
                awaitItem() // Data

                underTest.togglePlayPause()

                verify(gateway).pause()
                assertThat(underTest.controlBlockedByCall.value).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that seekTo is refused when a call is ongoing`() = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

        underTest.seekTo(5_000L)

        verify(gateway, never()).seekTo(any())
        assertThat(underTest.controlBlockedByCall.value).isEqualTo(triggered)
    }

    @Test
    fun `test that skipToNext is refused when a call is ongoing`() = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

        underTest.skipToNext()

        verify(gateway, never()).skipToNext()
        assertThat(underTest.controlBlockedByCall.value).isEqualTo(triggered)
    }

    @Test
    fun `test that sessionEnded emits when the service ends an active session`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(isPlaying = true))
            awaitItem() // Data — an active session was shown

            gatewayPlayerState.emit(
                audioControllerState(mediaItemCount = 1, isIdle = true, isSessionEnded = true)
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(underTest.sessionEnded.value).isTrue()
    }

    @Test
    fun `test that sessionEnded stays false when no active session was ever shown`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading

            gatewayPlayerState.emit(
                audioControllerState(mediaItemCount = 0, isIdle = true, isSessionEnded = true)
            )
            expectNoEvents()
        }

        assertThat(underTest.sessionEnded.value).isFalse()
    }

    @Test
    fun `test that startPlayback clears a previous session-ended signal`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading
            gatewayPlayerState.emit(audioControllerState(isPlaying = true))
            awaitItem() // Data
            gatewayPlayerState.emit(
                audioControllerState(mediaItemCount = 1, isIdle = true, isSessionEnded = true)
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(underTest.sessionEnded.value).isTrue()

        val intent = mock<Intent>().apply {
            whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
            whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
        }
        underTest.startPlayback(intent)

        assertThat(underTest.sessionEnded.value).isFalse()
    }

    @Test
    fun `test that startPlayback resets uiState to Loading when the previous session has ended`() =
        runTest {
            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(isPlaying = true, title = "Old"))
                awaitItem() // Data for the previous track
                gatewayPlayerState.emit(
                    audioControllerState(mediaItemCount = 1, isIdle = true, isSessionEnded = true)
                )
                skipItems(1) // Data updated with the ended flags

                underTest.startPlayback(intent)

                assertThat(awaitItem()).isEqualTo(AudioPlayerUiState.Loading)
                expectNoEvents()
            }
        }

    @Test
    fun `test that startPlayback keeps the current uiState when the previous session is still live`() =
        runTest {
            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }

            underTest.uiState.test {
                awaitItem() // Loading
                gatewayPlayerState.emit(audioControllerState(isPlaying = true, title = "Live"))
                awaitItem() // Data for the still-playing track

                underTest.startPlayback(intent)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(underTest.uiState.value).isInstanceOf(AudioPlayerUiState.Data::class.java)
        }

    @Test
    fun `test that startPlayback drains a pending resume event when rebuilding the playlist`() =
        runTest {
            audioResumeEventQueue.emit(AudioResumeEvent(mediaHandle = 999L, resumePositionMs = 5_000L))
            val intent = mock<Intent>().apply {
                whenever(getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(true)
                whenever(getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, -1)).thenReturn(-1)
            }

            underTest.startPlayback(intent)

            assertThat(audioResumeEventQueue.consume()).isNull()
        }

    @Test
    fun `test that hasActiveSession reflects whether the queue holds items`() = runTest {
        underTest.hasActiveSession.test {
            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 0, isIdle = true))
            assertThat(awaitItem()).isFalse()

            gatewayPlayerState.emit(audioControllerState(mediaItemCount = 2))
            assertThat(awaitItem()).isTrue()
        }
    }

    companion object {
        // Registered as a field (not @ExtendWith) so the tests can read the dispatcher's virtual
        // time and feed it to the ViewModel as its wall clock.
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = CoroutineMainDispatcherExtension()

        // Intent extra keys — must match values in AudioPlayerViewModel
        private const val INTENT_EXTRA_KEY_ADAPTER_TYPE = "adapterType"
        private const val INTENT_EXTRA_KEY_REBUILD_PLAYLIST = "REBUILD_PLAYLIST"
        private const val INTENT_EXTRA_KEY_CHAT_ID = "chatId"
        private const val INTENT_EXTRA_KEY_MSG_ID = "msgId"
        private const val URL_FILE_LINK = "URL_FILE_LINK"
        private const val URL_LOCAL_FILE_PATH = "URL_LOCAL_FILE_PATH"

        // Adapter type codes — must match values in AudioPlayerViewModel
        private const val OFFLINE_ADAPTER = 2004
        private const val FOLDER_LINK_ADAPTER = 2005
        private const val FILE_LINK_ADAPTER = 2019
        private const val FROM_CHAT = 2020
        private const val FROM_ALBUM_SHARING = 2041
    }
}

/**
 * Builds an [AudioControllerState] with [mediaItemCount] defaulting to 1: the ViewModel maps a
 * state with an empty queue to Loading, so tests describing an active player must always carry
 * at least one item.
 */
private fun audioControllerState(
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    shuffleEnabled: Boolean = false,
    mediaItemCount: Int = 1,
    isBuffering: Boolean = false,
    isIdle: Boolean = false,
    isSessionEnded: Boolean = false,
    title: String? = null,
    artist: String? = null,
    artworkUri: String? = null,
    currentMediaItemId: String? = null,
    playbackSpeed: Float = 1f,
    currentMediaItemHandle: Long? = null,
    currentQueueIndex: Int = 0,
    lastItemTransitionWasAuto: Boolean = false,
    hasNext: Boolean = false,
): AudioControllerState = AudioControllerState(
    isPlaying = isPlaying,
    currentPositionMs = currentPositionMs,
    durationMs = durationMs,
    repeatMode = repeatMode,
    shuffleEnabled = shuffleEnabled,
    mediaItemCount = mediaItemCount,
    isBuffering = isBuffering,
    isIdle = isIdle,
    isSessionEnded = isSessionEnded,
    title = title,
    artist = artist,
    artworkUri = artworkUri,
    currentMediaItemId = currentMediaItemId,
    playbackSpeed = playbackSpeed,
    currentMediaItemHandle = currentMediaItemHandle,
    currentQueueIndex = currentQueueIndex,
    lastItemTransitionWasAuto = lastItemTransitionWasAuto,
    hasNext = hasNext,
)
