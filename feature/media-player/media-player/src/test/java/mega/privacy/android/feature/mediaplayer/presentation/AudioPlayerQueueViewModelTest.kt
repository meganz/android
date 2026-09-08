package mega.privacy.android.feature.mediaplayer.presentation

import androidx.media3.common.Player
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.model.AudioControllerState
import mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AudioPlayerQueueViewModelTest {

    private lateinit var underTest: AudioPlayerQueueViewModel
    private lateinit var playerStateFlow: MutableSharedFlow<AudioControllerState>
    private lateinit var continuousPlaybackFlow: MutableSharedFlow<Boolean>

    private val gateway = mock<AudioMediaControllerGateway>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()
    private val monitorAudioContinuousPlaybackEnabledUseCase =
        mock<MonitorAudioContinuousPlaybackEnabledUseCase>()
    private val setAudioContinuousPlaybackEnabledUseCase =
        mock<SetAudioContinuousPlaybackEnabledUseCase>()

    @BeforeEach
    fun setUp() {
        // replay = 1 ensures the collected value is delivered even if the coroutine subscribes
        // after the emit call (due to lazy test-scheduler ordering).
        playerStateFlow = MutableSharedFlow(replay = 1)
        continuousPlaybackFlow = MutableSharedFlow(replay = 1)
        continuousPlaybackFlow.tryEmit(true)
        whenever(gateway.playerState).thenReturn(playerStateFlow)
        whenever(monitorAudioContinuousPlaybackEnabledUseCase()).thenReturn(continuousPlaybackFlow)
        initTestClass()
    }

    private fun initTestClass() {
        underTest = AudioPlayerQueueViewModel(
            gateway = gateway,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            monitorAudioContinuousPlaybackEnabledUseCase = monitorAudioContinuousPlaybackEnabledUseCase,
            setAudioContinuousPlaybackEnabledUseCase = setAudioContinuousPlaybackEnabledUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            gateway,
            getNodeByHandleUseCase,
            isParticipatingInChatCallUseCase,
            monitorAudioContinuousPlaybackEnabledUseCase,
            setAudioContinuousPlaybackEnabledUseCase,
        )
    }

    private suspend fun ReceiveTurbine<AudioPlayerQueueUiState>.awaitDataState(): AudioPlayerQueueUiState.Data =
        awaitItem() as AudioPlayerQueueUiState.Data

    private suspend fun ReceiveTurbine<AudioPlayerQueueUiState>.awaitDataStateUntil(
        predicate: (AudioPlayerQueueUiState.Data) -> Boolean,
    ): AudioPlayerQueueUiState.Data {
        while (true) {
            val state = awaitDataState()
            if (predicate(state)) return state
        }
    }

    @Test
    fun `test that uiState emits Loading before first controller state arrives`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(AudioPlayerQueueUiState.Loading)
    }

    @Test
    fun `test that uiState maps queue items and index from controller state`() = runTest {
        val items = listOf(
            AudioQueueItem(mediaId = "a", title = "Track A", artist = null),
            AudioQueueItem(mediaId = "b", title = "Track B", artist = "Artist B"),
        )
        underTest.uiState.test {
            awaitItem() // Loading state

            playerStateFlow.emit(
                AudioControllerState(queueItems = items, currentQueueIndex = 1)
            )

            val state = awaitDataState()
            assertThat(state.items).isEqualTo(items)
            assertThat(state.currentQueueIndex).isEqualTo(1)
        }
    }

    @Test
    fun `test that uiState maps isContinuousPlayback true when the preference is enabled`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading state

                continuousPlaybackFlow.emit(true)
                playerStateFlow.emit(AudioControllerState())

                val state = awaitDataStateUntil { it.isContinuousPlayback }
                assertThat(state.isContinuousPlayback).isTrue()
            }
        }

    @Test
    fun `test that uiState maps isContinuousPlayback false when the preference is disabled`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading state

                continuousPlaybackFlow.emit(false)
                playerStateFlow.emit(AudioControllerState())

                val state = awaitDataStateUntil { !it.isContinuousPlayback }
                assertThat(state.isContinuousPlayback).isFalse()
            }
        }

    @Test
    fun `test that uiState maps isContinuousPlayback independently of the repeat mode`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading state

            continuousPlaybackFlow.emit(false)
            playerStateFlow.emit(AudioControllerState(repeatMode = Player.REPEAT_MODE_ONE))

            val state = awaitDataStateUntil { !it.isContinuousPlayback }
            assertThat(state.isContinuousPlayback).isFalse()
        }
    }

    @Test
    fun `test that uiState enriches current queue item with controller title and artist`() =
        runTest {
            val items = listOf(
                AudioQueueItem(mediaId = "a", title = "Raw A", artist = "Raw Artist A"),
                AudioQueueItem(mediaId = "b", title = "Raw B", artist = "Raw Artist B"),
            )
            underTest.uiState.test {
                awaitItem() // Loading state

                playerStateFlow.emit(
                    AudioControllerState(
                        queueItems = items,
                        currentQueueIndex = 1,
                        title = "Metadata title",
                        artist = "Metadata artist",
                    )
                )

                val state = awaitDataState()
                assertThat(state.items[1].title).isEqualTo("Metadata title")
                assertThat(state.items[1].artist).isEqualTo("Metadata artist")
                assertThat(state.items[0]).isEqualTo(items[0])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState maps currentThumbnailData from current media item handle`() = runTest {
        underTest.uiState.test {
            awaitItem() // Loading state

            playerStateFlow.emit(AudioControllerState(currentMediaItemHandle = 42L))

            val state = awaitDataState()
            assertThat(state.currentThumbnailData).isEqualTo(ThumbnailRequest.fromHandle(42L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState maps current title artist and artwork from controller state`() =
        runTest {
            underTest.uiState.test {
                awaitItem() // Loading state

                playerStateFlow.emit(
                    AudioControllerState(
                        title = "Song title",
                        artist = "Song artist",
                        artworkUri = "content://artwork/1",
                    )
                )

                val state = awaitDataState()
                assertThat(state.currentTitle).isEqualTo("Song title")
                assertThat(state.currentArtist).isEqualTo("Song artist")
                assertThat(state.currentArtworkUri).isEqualTo("content://artwork/1")
            }
        }

    @Test
    fun `test that uiState fetches playingFromName when media item changes`() = runTest {
        val parentNode = mock<FileNode> { on { name }.thenReturn("My Music") }
        val currentNode = mock<FileNode> {
            on { parentId }.thenReturn(NodeId(99L))
        }
        whenever(getNodeByHandleUseCase(42L)).thenReturn(currentNode)
        whenever(getNodeByHandleUseCase(99L)).thenReturn(parentNode)

        underTest.uiState.test {
            awaitItem() // Loading state

            playerStateFlow.emit(
                AudioControllerState(
                    currentMediaItemId = "item-1",
                    currentMediaItemHandle = 42L,
                )
            )

            awaitDataState() // state update with null playingFromName (immediate from flatMapLatest)
            val state = awaitDataState() // playingFromName resolved
            assertThat(state.playingFromName).isEqualTo("My Music")
        }
    }

    @Test
    fun `test that uiState does not re-fetch playingFromName when same handle is emitted again`() =
        runTest {
            val parentNode = mock<FileNode> { on { name }.thenReturn("Folder") }
            val currentNode = mock<FileNode> {
                on { parentId }.thenReturn(NodeId(5L))
            }
            whenever(getNodeByHandleUseCase(1L)).thenReturn(currentNode)
            whenever(getNodeByHandleUseCase(5L)).thenReturn(parentNode)

            underTest.uiState.test {
                awaitItem() // Loading state

                playerStateFlow.emit(
                    AudioControllerState(
                        currentMediaItemId = "same-id",
                        currentMediaItemHandle = 1L
                    )
                )
                awaitDataState() // state update with null playingFromName
                awaitDataState() // playingFromName resolved

                // Emit the same handle again with an unrelated field changed: currentQueueIndex
                // makes the resulting Data differ (StateFlow would swallow an identical value),
                // while distinctUntilChanged on the handle still suppresses the re-fetch.
                playerStateFlow.emit(
                    AudioControllerState(
                        currentMediaItemId = "same-id",
                        currentMediaItemHandle = 1L,
                        currentQueueIndex = 1,
                    )
                )
                awaitDataState() // state update (no playingFromName re-fetch expected)

                verify(getNodeByHandleUseCase, times(1))(1L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState emits null playingFromName when node fetch throws`() = runTest {
        whenever(getNodeByHandleUseCase(7L)).thenThrow(RuntimeException("node fetch failed"))

        underTest.uiState.test {
            awaitItem() // Loading state

            playerStateFlow.emit(
                AudioControllerState(
                    currentMediaItemId = "item-err",
                    currentMediaItemHandle = 7L,
                )
            )

            val state = awaitDataState()
            assertThat(state.playingFromName).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState re-fetches playingFromName when media item handle changes`() = runTest {
        val parentA = mock<FileNode> { on { name }.thenReturn("Folder A") }
        val nodeA = mock<FileNode> {
            on { parentId }.thenReturn(NodeId(10L))
        }
        val parentB = mock<FileNode> { on { name }.thenReturn("Folder B") }
        val nodeB = mock<FileNode> {
            on { parentId }.thenReturn(NodeId(20L))
        }
        whenever(getNodeByHandleUseCase(1L)).thenReturn(nodeA)
        whenever(getNodeByHandleUseCase(10L)).thenReturn(parentA)
        whenever(getNodeByHandleUseCase(2L)).thenReturn(nodeB)
        whenever(getNodeByHandleUseCase(20L)).thenReturn(parentB)

        underTest.uiState.test {
            awaitItem() // Loading state

            playerStateFlow.emit(
                AudioControllerState(currentMediaItemId = "a", currentMediaItemHandle = 1L)
            )
            awaitDataStateUntil { it.playingFromName == "Folder A" }

            playerStateFlow.emit(
                AudioControllerState(currentMediaItemId = "b", currentMediaItemHandle = 2L)
            )
            awaitDataStateUntil { it.playingFromName == "Folder B" }

            verify(getNodeByHandleUseCase)(2L)
            verify(getNodeByHandleUseCase)(20L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that seekToQueueItem calls seekToMediaItem on gateway with the given index`() =
        runTest {
            underTest.seekToQueueItem(3)
            verify(gateway).seekToMediaItem(3)
        }

    @Test
    fun `test that seekToQueueItem is refused when a call is ongoing`() = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

        underTest.uiState.test {
            awaitItem() // Loading
            playerStateFlow.emit(AudioControllerState())
            awaitDataState()

            underTest.seekToQueueItem(3)

            awaitDataStateUntil { it.controlBlockedByCall == triggered }
        }
        verify(gateway, never()).seekToMediaItem(any())
    }

    @Test
    fun `test that onControlBlockedByCallConsumed clears the announcement`() = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

        underTest.uiState.test {
            awaitItem() // Loading
            playerStateFlow.emit(AudioControllerState())
            awaitDataState()
            underTest.seekToQueueItem(3)
            awaitDataStateUntil { it.controlBlockedByCall == triggered }

            underTest.onControlBlockedByCallConsumed()

            awaitDataStateUntil { it.controlBlockedByCall == consumed }
        }
    }

    @Test
    fun `test that setContinuousPlayback saves the preference when enabled is true`() = runTest {
        underTest.setContinuousPlayback(true)
        verify(setAudioContinuousPlaybackEnabledUseCase)(true)
    }

    @Test
    fun `test that setContinuousPlayback saves the preference when enabled is false`() = runTest {
        underTest.setContinuousPlayback(false)
        verify(setAudioContinuousPlaybackEnabledUseCase)(false)
    }

    @Test
    fun `test that moveQueueItem calls moveMediaItem on gateway with the given indices`() = runTest {
        underTest.moveQueueItem(1, 3)
        verify(gateway).moveMediaItem(1, 3)
    }
}
