package mega.privacy.android.app.mediaplayer.gateway

import android.content.Context
import android.view.TextureView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoMediaControllerFacadeTest {

    private val context = mock<Context>()
    private val controller = mock<MediaController>()

    /**
     * Drives the facade's position poll. Kept separate from the one `runTest` owns and advanced
     * only by the tests that exercise the poll: a test dispatcher skips delays, so on a scheduler
     * that something else advances to idle the poll's `while (isActive)` loop becomes an unbounded
     * allocation loop that exhausts the test worker's heap.
     */
    private lateinit var playerScheduler: TestCoroutineScheduler

    private lateinit var underTest: VideoMediaControllerFacade

    @BeforeEach
    fun setUp() {
        reset(context, controller)
        playerScheduler = TestCoroutineScheduler()
        underTest = VideoMediaControllerFacade(
            context = context,
            mainDispatcher = StandardTestDispatcher(playerScheduler),
        )
    }

    private fun mediaItem(mediaId: String) = MediaItem.Builder().setMediaId(mediaId).build()

    @AfterEach
    fun tearDown() {
        // Guarded so a failure in setUp surfaces as itself rather than as an
        // UninitializedPropertyAccessException raised here.
        if (::underTest.isInitialized) underTest.release()
    }

    /**
     * A session connection cannot be established in a JVM test, so the controller is installed
     * through the same path a real connection takes. Assigning the field directly would skip
     * [VideoMediaControllerFacade.onControllerConnected], and with it the listener registration
     * that the teardown tests then assert is undone.
     */
    private fun connected() {
        val future = mock<ListenableFuture<MediaController>>()
        underTest.controllerFuture = future
        underTest.onControllerConnected(future, controller)
    }

    /** Only the position is read from it; the rest is what an unset, ad-free position carries. */
    private fun positionInfo(positionMs: Long) = Player.PositionInfo(
        null,
        0,
        null,
        null,
        0,
        positionMs,
        positionMs,
        C.INDEX_UNSET,
        C.INDEX_UNSET,
    )

    @ParameterizedTest(name = "playbackState={0} -> buffering={1}, ended={2}, ready={3}")
    @MethodSource("playbackStates")
    fun `test that onPlaybackStateChanged maps the raw player state`(
        state: Int,
        buffering: Boolean,
        ended: Boolean,
        ready: Boolean,
    ) = runTest {
        underTest.playerListener.onPlaybackStateChanged(state)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.isBuffering).isEqualTo(buffering)
            assertThat(emitted.isEnded).isEqualTo(ended)
            assertThat(emitted.isReady).isEqualTo(ready)
        }
    }

    @Test
    fun `test that isPlaying is true when the player is ready and playWhenReady`() = runTest {
        underTest.playerListener.onPlaybackStateChanged(Player.STATE_READY)
        underTest.playerListener.onPlayWhenReadyChanged(
            true,
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )

        underTest.playerState.test {
            assertThat(awaitItem().isPlaying).isTrue()
        }
    }

    @Test
    fun `test that isPlaying is false when the player is ready but paused`() = runTest {
        underTest.playerListener.onPlaybackStateChanged(Player.STATE_READY)
        underTest.playerListener.onPlayWhenReadyChanged(
            false,
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.isPlaying).isFalse()
            assertThat(emitted.pausedByUserRequest).isTrue()
        }
    }

    @Test
    fun `test that a pause the user did not ask for is not recorded as a user request`() = runTest {
        underTest.playerListener.onPlayWhenReadyChanged(
            false,
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
        )

        underTest.playerState.test {
            assertThat(awaitItem().pausedByUserRequest).isFalse()
        }
    }

    @Test
    fun `test that isPlaying is false when playWhenReady is set while still buffering`() = runTest {
        underTest.playerListener.onPlayWhenReadyChanged(
            true,
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )
        underTest.playerListener.onPlaybackStateChanged(Player.STATE_BUFFERING)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.playWhenReady).isTrue()
            assertThat(emitted.isPlaying).isFalse()
        }
    }

    @Test
    fun `test that onIsPlayingChanged samples position and duration while playing`() = runTest {
        whenever(controller.currentPosition).thenReturn(1_200L)
        whenever(controller.duration).thenReturn(9_000L)
        connected()

        underTest.playerListener.onIsPlayingChanged(true)
        playerScheduler.advanceTimeBy(1_100)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.currentPositionMs).isEqualTo(1_200L)
            assertThat(emitted.durationMs).isEqualTo(9_000L)
        }
    }

    @Test
    fun `test that onIsPlayingChanged stops sampling when playback pauses`() = runTest {
        connected()

        underTest.playerListener.onIsPlayingChanged(true)
        playerScheduler.advanceTimeBy(1_100)
        // Without this the assertion below would also hold if the poll had never started.
        verify(controller, atLeastOnce()).currentPosition
        clearInvocations(controller)

        underTest.playerListener.onIsPlayingChanged(false)
        // The one final sample on the way out is expected; what must not happen is the poll
        // continuing to fire after it.
        clearInvocations(controller)
        playerScheduler.advanceTimeBy(5_000)

        verify(controller, never()).currentPosition
        verify(controller, never()).duration
    }

    @Test
    fun `test that onIsPlayingChanged clamps a sampled position and duration below zero`() =
        runTest {
            // ExoPlayer reports C.TIME_UNSET, a large negative, for a duration it does not know yet.
            whenever(controller.currentPosition).thenReturn(-1L)
            whenever(controller.duration).thenReturn(C.TIME_UNSET)
            connected()

            underTest.playerListener.onIsPlayingChanged(true)
            playerScheduler.advanceTimeBy(600)

            // Zero is also the initial value of both fields, so the poll has to be shown to have
            // run for the assertions below to mean anything.
            verify(controller, atLeastOnce()).currentPosition
            verify(controller, atLeastOnce()).duration
            underTest.playerState.test {
                val emitted = awaitItem()
                assertThat(emitted.currentPositionMs).isEqualTo(0L)
                assertThat(emitted.durationMs).isEqualTo(0L)
            }
        }

    @Test
    fun `test that onMediaItemTransition clears the video size of the previous item`() = runTest {
        underTest.playerListener.onVideoSizeChanged(VideoSize(1920, 1080))

        underTest.playerListener.onMediaItemTransition(
            MediaItem.Builder().setMediaId("123").build(),
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
        )

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.currentMediaHandle).isEqualTo(123L)
            assertThat(emitted.transitionUpdatesName).isTrue()
            assertThat(emitted.videoWidth).isEqualTo(0)
            assertThat(emitted.videoHeight).isEqualTo(0)
        }
    }

    @Test
    fun `test that onMediaItemTransition does not update the name when the item repeated`() =
        runTest {
            underTest.playerListener.onMediaItemTransition(
                MediaItem.Builder().setMediaId("123").build(),
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            )

            underTest.playerState.test {
                assertThat(awaitItem().transitionUpdatesName).isFalse()
            }
        }

    @Test
    fun `test that onMediaItemTransition carries the name of the item the player moved to`() =
        runTest {
            underTest.playerListener.onMediaItemTransition(
                MediaItem.Builder()
                    .setMediaId("123")
                    .setMediaMetadata(
                        MediaMetadata.Builder().setDisplayTitle("holiday.mp4").build()
                    )
                    .build(),
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )

            // The display title rather than the title: the queue builder writes the node's name
            // there precisely because the container's own metadata overwrites the title.
            underTest.playerState.test {
                assertThat(awaitItem().currentMediaName).isEqualTo("holiday.mp4")
            }
        }

    @Test
    fun `test that onVideoSizeChanged ignores a zero size`() = runTest {
        underTest.playerListener.onVideoSizeChanged(VideoSize(1920, 1080))

        underTest.playerListener.onVideoSizeChanged(VideoSize.UNKNOWN)

        // A zero size arrives between items and would collapse the video frame to nothing.
        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.videoWidth).isEqualTo(1920)
            assertThat(emitted.videoHeight).isEqualTo(1080)
        }
    }

    @Test
    fun `test that onPlayerError does not take down the state stream`() = runTest {
        underTest.playerListener.onPlaybackStateChanged(Player.STATE_READY)

        underTest.playerListener.onPlayerError(
            PlaybackException("io", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        )

        // Nothing consumes errors yet, so the contract is that one leaves the rest of the state
        // standing rather than resetting or completing the stream.
        underTest.playerState.test {
            assertThat(awaitItem().isReady).isTrue()
        }
    }

    @Test
    fun `test that setPlaySources prepares the controller with the queue`() {
        connected()
        val items = listOf(mediaItem("1"), mediaItem("2"))

        underTest.setPlaySources(items, 1)

        // Preparing before the items are set would prepare the previous one, or nothing at all.
        val inOrder = inOrder(controller)
        inOrder.verify(controller).setMediaItems(items, 1, 0L)
        inOrder.verify(controller).prepare()
    }

    @Test
    fun `test that setPlaySources ignores an empty queue`() {
        connected()

        underTest.setPlaySources(emptyList(), 0)

        // An empty queue says the sibling search came back with nothing, not that the video the
        // user opened should stop.
        verify(controller, never()).setMediaItems(any<List<MediaItem>>(), any(), any())
        verify(controller, never()).prepare()
    }

    @Test
    fun `test that setPlaySources clamps a start index outside the queue`() {
        connected()
        val items = listOf(mediaItem("1"), mediaItem("2"))

        underTest.setPlaySources(items, 7)

        verify(controller).setMediaItems(items, 1, 0L)
    }

    @Test
    fun `test that setPlaySources clamps a negative start index to the first item`() {
        connected()
        val items = listOf(mediaItem("1"), mediaItem("2"))

        underTest.setPlaySources(items, -1)

        // The clamp has two sides; only testing the upper one lets the lower regress unseen.
        verify(controller).setMediaItems(items, 0, 0L)
    }

    @Test
    fun `test that setPlaySources grows the queue around the item already playing`() {
        connected()
        whenever(controller.mediaItemCount).thenReturn(1)
        whenever(controller.currentMediaItem).thenReturn(mediaItem("2"))
        whenever(controller.playbackState).thenReturn(Player.STATE_READY)
        val items = listOf(mediaItem("1"), mediaItem("2"), mediaItem("3"))

        underTest.setPlaySources(items, 1)

        // setMediaItems resets the player to idle even when it lands on the item already on
        // screen, which costs a visible re-buffer.
        verify(controller, never()).setMediaItems(any<List<MediaItem>>(), any(), any())
        verify(controller).addMediaItems(0, items.subList(0, 1))
        verify(controller).addMediaItems(2, items.subList(2, 3))
    }

    @Test
    fun `test that setPlaySources replaces the queue when the player is on a different item`() {
        connected()
        whenever(controller.mediaItemCount).thenReturn(1)
        whenever(controller.currentMediaItem).thenReturn(mediaItem("9"))
        whenever(controller.playbackState).thenReturn(Player.STATE_READY)
        val items = listOf(mediaItem("1"), mediaItem("2"))

        underTest.setPlaySources(items, 0)

        verify(controller).setMediaItems(items, 0, 0L)
        verify(controller, never()).addMediaItems(any<Int>(), any<List<MediaItem>>())
    }

    @Test
    fun `test that setPlaySources replaces the queue when the player has not prepared the item`() {
        connected()
        whenever(controller.mediaItemCount).thenReturn(1)
        whenever(controller.currentMediaItem).thenReturn(mediaItem("1"))
        whenever(controller.playbackState).thenReturn(Player.STATE_IDLE)
        val items = listOf(mediaItem("1"), mediaItem("2"))

        underTest.setPlaySources(items, 0)

        // Growing an idle timeline leaves the player idle: nothing would ever prepare it.
        verify(controller).setMediaItems(items, 0, 0L)
    }

    @Test
    fun `test that play starts playback on the controller`() {
        connected()

        underTest.play()

        verify(controller).play()
    }

    @Test
    fun `test that pause stops playback on the controller`() {
        connected()

        underTest.pause()

        verify(controller).pause()
    }

    @Test
    fun `test that pause drops a play that was waiting for the connection`() {
        // Pausing before the controller arrives cannot reach the player, but it must still beat
        // the held play, or the video starts the moment the connection lands.
        underTest.play()
        underTest.pause()

        connected()

        verify(controller, never()).play()
    }

    @Test
    fun `test that seekTo moves the controller to the position`() {
        connected()

        underTest.seekTo(72_000L)

        verify(controller).seekTo(72_000L)
    }

    @Test
    fun `test that seekBack and seekForward use the player's own increment`() {
        connected()

        // Not arithmetic on a sampled position: the player knows where it is and clamps at both
        // ends, and the increment is the one the service built it with.
        underTest.seekBack()
        underTest.seekForward()

        verify(controller).seekBack()
        verify(controller).seekForward()
    }

    @Test
    fun `test that skipToPrevious and skipToNext move the controller between items`() {
        connected()

        underTest.skipToPrevious()
        underTest.skipToNext()

        verify(controller).seekToPrevious()
        verify(controller).seekToNext()
    }

    @Test
    fun `test that the transport commands do nothing before the controller arrives`() {
        underTest.seekBack()
        underTest.seekForward()
        underTest.skipToPrevious()
        underTest.skipToNext()

        verify(controller, never()).seekBack()
        verify(controller, never()).seekForward()
        verify(controller, never()).seekToPrevious()
        verify(controller, never()).seekToNext()
    }

    @Test
    fun `test that a position discontinuity refreshes the position and duration`() = runTest {
        whenever(controller.duration).thenReturn(225_000L)
        connected()

        // Polling only runs while playing, so without this a seek made from the paused screen
        // would leave the scrub bar showing where the drag began. Driven from the listener rather
        // than from seekTo so that a seek this facade did not issue — a media button, or another
        // controller on the player the service shares — is picked up too.
        underTest.playerListener.onPositionDiscontinuity(
            positionInfo(0L),
            positionInfo(72_000L),
            Player.DISCONTINUITY_REASON_SEEK,
        )

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.currentPositionMs).isEqualTo(72_000L)
            assertThat(emitted.durationMs).isEqualTo(225_000L)
        }
    }

    @Test
    fun `test that a position discontinuity keeps the duration the player already reported`() =
        runTest {
            whenever(controller.duration).thenReturn(225_000L)
            connected()
            underTest.playerListener.onPositionDiscontinuity(
                positionInfo(0L),
                positionInfo(72_000L),
                Player.DISCONTINUITY_REASON_SEEK,
            )

            // A player that has not worked out the length answers C.TIME_UNSET. Flooring that to
            // zero would erase a length already known, and the scrub bar refuses to seek without
            // one, with nothing left running to repair it while playback is paused.
            whenever(controller.duration).thenReturn(C.TIME_UNSET)
            underTest.playerListener.onPositionDiscontinuity(
                positionInfo(72_000L),
                positionInfo(90_000L),
                Player.DISCONTINUITY_REASON_SEEK,
            )

            underTest.playerState.test {
                val emitted = awaitItem()
                assertThat(emitted.currentPositionMs).isEqualTo(90_000L)
                assertThat(emitted.durationMs).isEqualTo(225_000L)
            }
        }

    @Test
    fun `test that seekTo does nothing before the controller arrives`() {
        underTest.seekTo(72_000L)

        verify(controller, never()).seekTo(any<Long>())
    }

    @Test
    fun `test that attachSurface binds the texture view to the controller`() {
        val textureView = mock<TextureView>()
        connected()

        underTest.attachSurface(textureView)

        verify(controller).setVideoTextureView(textureView)
    }

    @Test
    fun `test that clearSurface detaches the video output from the controller`() {
        val textureView = mock<TextureView>()
        connected()
        underTest.attachSurface(textureView)

        underTest.clearSurface()

        verify(controller).clearVideoTextureView(textureView)
    }

    @Test
    fun `test that clearSurface leaves a surface this instance did not attach alone`() {
        connected()

        underTest.clearSurface()

        // The player belongs to the service and may already be rendering a successor screen's
        // surface; clearing whatever it currently holds would black that screen out.
        verify(controller, never()).clearVideoSurface()
        verify(controller, never()).clearVideoTextureView(any())
    }

    @Test
    fun `test that the content commands are applied once the controller connects`() {
        val textureView = mock<TextureView>()
        val future = mock<ListenableFuture<MediaController>>()
        underTest.controllerFuture = future

        val items = listOf(mediaItem("123"))
        underTest.setPlaySources(items, 0)
        underTest.play()
        underTest.attachSurface(textureView)
        underTest.onControllerConnected(future, controller)

        // Nothing retries these, so losing one leaves a player that never starts or never shows a
        // picture.
        verify(controller).setVideoTextureView(textureView)
        verify(controller).setMediaItems(items, 0, 0L)
        verify(controller).prepare()
        verify(controller).play()
    }

    @Test
    fun `test that clearSurface drops a surface that is still waiting for the controller`() {
        val textureView = mock<TextureView>()
        val future = mock<ListenableFuture<MediaController>>()
        underTest.controllerFuture = future

        underTest.attachSurface(textureView)
        underTest.clearSurface()
        underTest.onControllerConnected(future, controller)

        // The owner's TextureView is gone by then; binding it would hold a detached view.
        verify(controller, never()).setVideoTextureView(any())
    }

    @Test
    fun `test that a failed connection reports the player as unavailable`() = runTest {
        val future = mock<ListenableFuture<MediaController>>()
        underTest.controllerFuture = future
        underTest.setPlaySources(listOf(mediaItem("123")), 0)
        underTest.play()
        underTest.attachSurface(mock<TextureView>())

        underTest.onControllerConnectionFailed(future, RuntimeException("service unreachable"))

        // Nothing retries, so holding the commands would keep them for a controller that is not
        // coming while the owner has no way to tell the screen will stay black.
        underTest.playerState.test {
            assertThat(awaitItem().isPlayerUnavailable).isTrue()
        }
        underTest.onControllerConnected(future, controller)
        verify(controller, never()).setMediaItems(any<List<MediaItem>>(), any(), any())
        verify(controller, never()).play()
        verify(controller, never()).setVideoTextureView(any())
    }

    @Test
    fun `test that onIsPlayingChanged samples the position once more before the poll stops`() =
        runTest {
            whenever(controller.currentPosition).thenReturn(7_500L)
            whenever(controller.duration).thenReturn(9_000L)
            connected()

            underTest.playerListener.onIsPlayingChanged(false)

            // The poll can be a whole interval behind, and this value stands until playback
            // resumes, so it is the one a resume position would be taken from.
            underTest.playerState.test {
                assertThat(awaitItem().currentPositionMs).isEqualTo(7_500L)
            }
        }

    @Test
    fun `test that a stale connection result is released instead of installed`() {
        val staleController = mock<MediaController>()
        underTest.controllerFuture = mock<ListenableFuture<MediaController>>()

        underTest.onControllerConnected(mock<ListenableFuture<MediaController>>(), staleController)

        verify(staleController).release()
        verify(staleController, never()).addListener(underTest.playerListener)
        assertThat(underTest.controller).isNull()
    }

    @Test
    fun `test that release detaches the video surface and drops the controller`() {
        val textureView = mock<TextureView>()
        connected()
        underTest.attachSurface(textureView)

        underTest.release()

        verify(controller).removeListener(underTest.playerListener)
        verify(controller).clearVideoTextureView(textureView)
        assertThat(underTest.controller).isNull()
    }

    @Test
    fun `test that release leaves a surface this instance did not attach alone`() {
        connected()

        underTest.release()

        // Two players can overlap while the outgoing screen is torn down.
        verify(controller, never()).clearVideoSurface()
        verify(controller, never()).clearVideoTextureView(any())
    }

    @Test
    fun `test that release detaches the surface when removing the listener fails`() {
        val textureView = mock<TextureView>()
        connected()
        underTest.attachSurface(textureView)
        doThrow(IllegalStateException("controller already released"))
            .whenever(controller).removeListener(any())

        underTest.release()

        // A throw on the first teardown step must not strand the surface on a dead controller.
        verify(controller).clearVideoTextureView(textureView)
    }

    @Test
    fun `test that release is idempotent`() {
        connected()
        underTest.release()
        clearInvocations(controller)

        underTest.release()

        verify(controller, never()).clearVideoSurface()
    }

    private fun playbackStates(): Stream<Arguments> = Stream.of(
        Arguments.of(Player.STATE_BUFFERING, true, false, false),
        Arguments.of(Player.STATE_ENDED, false, true, false),
        Arguments.of(Player.STATE_READY, false, false, true),
        Arguments.of(Player.STATE_IDLE, false, false, false),
    )
}
