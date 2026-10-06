package mega.privacy.android.app.mediaplayer.gateway

import android.net.Uri
import android.view.TextureView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videoplayer.MediaPlayerManager
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
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

    private val player = mock<ExoPlayer>()
    private val mediaPlayerManager = mock<MediaPlayerManager>()

    /**
     * Drives the facade's position poll. Kept separate from the one `runTest` owns and advanced
     * only by the tests that exercise the poll: a test dispatcher skips delays, so on a scheduler
     * that something else advances to idle the poll's `while (isActive)` loop becomes an unbounded
     * allocation loop that exhausts the test worker's heap.
     */
    private lateinit var playerScheduler: TestCoroutineScheduler

    /**
     * The nine callbacks the facade hands to `createPlayer`, captured in the order the signature
     * declares them. Driving these is the only way to exercise the translation: the facade keeps
     * no listener of its own.
     *
     * Only the four the facade acts on are read back; the rest are empty lambdas with nothing to
     * assert. Reordering `createPlayer` surfaces as a ClassCastException, and adding or removing a
     * parameter stops the stub matching at all.
     */
    private lateinit var callbacks: Array<Any>

    private lateinit var underTest: VideoMediaControllerFacade

    @BeforeEach
    fun setUp() {
        reset(player, mediaPlayerManager)
        playerScheduler = TestCoroutineScheduler()
        whenever(
            mediaPlayerManager.createPlayer(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        ).thenAnswer { invocation ->
            callbacks = invocation.arguments
            player
        }
        underTest = VideoMediaControllerFacade(
            mediaPlayerManager = mediaPlayerManager,
            mainDispatcher = StandardTestDispatcher(playerScheduler),
        )
    }

    @AfterEach
    fun tearDown() {
        // Guarded so a failure in setUp surfaces as itself rather than as an
        // UninitializedPropertyAccessException raised here.
        if (::underTest.isInitialized) underTest.release()
    }

    @Suppress("UNCHECKED_CAST")
    private fun onMediaItemTransition() =
        callbacks[1] as (String?, Boolean) -> Unit

    @Suppress("UNCHECKED_CAST")
    private fun onPlayWhenReadyChanged() =
        callbacks[3] as (MediaPlaybackState, Boolean) -> Unit

    @Suppress("UNCHECKED_CAST")
    private fun onPlaybackStateChanged() = callbacks[4] as (Int) -> Unit

    @Suppress("UNCHECKED_CAST")
    private fun onVideoSizeChanged() = callbacks[8] as (VideoSize) -> Unit

    @ParameterizedTest(name = "playbackState={0} -> buffering={1}, ended={2}, ready={3}")
    @MethodSource("playbackStates")
    fun `test that onPlaybackStateChanged maps the raw player state`(
        state: Int,
        buffering: Boolean,
        ended: Boolean,
        ready: Boolean,
    ) = runTest {
        onPlaybackStateChanged()(state)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.isBuffering).isEqualTo(buffering)
            assertThat(emitted.isEnded).isEqualTo(ended)
            assertThat(emitted.isReady).isEqualTo(ready)
        }
    }

    @Test
    fun `test that isPlaying is true when the player is ready and playWhenReady`() = runTest {
        onPlaybackStateChanged()(Player.STATE_READY)
        onPlayWhenReadyChanged()(MediaPlaybackState.Playing, false)

        underTest.playerState.test {
            assertThat(awaitItem().isPlaying).isTrue()
        }
    }

    @Test
    fun `test that isPlaying is false when the player is ready but paused`() = runTest {
        onPlaybackStateChanged()(Player.STATE_READY)
        onPlayWhenReadyChanged()(MediaPlaybackState.Paused, true)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.isPlaying).isFalse()
            assertThat(emitted.pausedByUserRequest).isTrue()
        }
    }

    @Test
    fun `test that isPlaying is false when playWhenReady is set while still buffering`() = runTest {
        onPlayWhenReadyChanged()(MediaPlaybackState.Playing, false)
        onPlaybackStateChanged()(Player.STATE_BUFFERING)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.playWhenReady).isTrue()
            assertThat(emitted.isPlaying).isFalse()
        }
    }

    @Test
    fun `test that onPlayWhenReadyChanged samples position and duration while playing`() =
        runTest {
            whenever(mediaPlayerManager.getCurrentPlayingPosition()).thenReturn(1_200L)
            whenever(mediaPlayerManager.getCurrentItemDuration()).thenReturn(9_000L)

            onPlayWhenReadyChanged()(MediaPlaybackState.Playing, false)
            playerScheduler.advanceTimeBy(1_100)

            underTest.playerState.test {
                val emitted = awaitItem()
                assertThat(emitted.currentPositionMs).isEqualTo(1_200L)
                assertThat(emitted.durationMs).isEqualTo(9_000L)
            }
        }

    @Test
    fun `test that onPlayWhenReadyChanged stops sampling when playback pauses`() = runTest {
        onPlayWhenReadyChanged()(MediaPlaybackState.Playing, false)
        playerScheduler.advanceTimeBy(1_100)
        // Without this the assertion below would also hold if the poll had never started.
        verify(mediaPlayerManager, atLeastOnce()).getCurrentPlayingPosition()
        clearInvocations(mediaPlayerManager)

        onPlayWhenReadyChanged()(MediaPlaybackState.Paused, true)
        playerScheduler.advanceTimeBy(5_000)

        verify(mediaPlayerManager, never()).getCurrentPlayingPosition()
        verify(mediaPlayerManager, never()).getCurrentItemDuration()
    }

    @Test
    fun `test that onPlayWhenReadyChanged clamps a sampled position and duration below zero`() =
        runTest {
            // ExoPlayer reports C.TIME_UNSET, a large negative, for a duration it does not know yet.
            whenever(mediaPlayerManager.getCurrentPlayingPosition()).thenReturn(-1L)
            whenever(mediaPlayerManager.getCurrentItemDuration()).thenReturn(C.TIME_UNSET)

            onPlayWhenReadyChanged()(MediaPlaybackState.Playing, false)
            playerScheduler.advanceTimeBy(600)

            // Zero is also the initial value of both fields, so the poll has to be shown to have
            // run for the assertions below to mean anything.
            verify(mediaPlayerManager, atLeastOnce()).getCurrentPlayingPosition()
            verify(mediaPlayerManager, atLeastOnce()).getCurrentItemDuration()
            underTest.playerState.test {
                val emitted = awaitItem()
                assertThat(emitted.currentPositionMs).isEqualTo(0L)
                assertThat(emitted.durationMs).isEqualTo(0L)
            }
        }

    @Test
    fun `test that onMediaItemTransition clears the video size of the previous item`() = runTest {
        onVideoSizeChanged()(VideoSize(1920, 1080))

        onMediaItemTransition()("123", true)

        underTest.playerState.test {
            val emitted = awaitItem()
            assertThat(emitted.currentMediaHandle).isEqualTo(123L)
            assertThat(emitted.transitionUpdatesName).isTrue()
            assertThat(emitted.videoWidth).isEqualTo(0)
            assertThat(emitted.videoHeight).isEqualTo(0)
        }
    }

    @Test
    fun `test that setPlaySource builds a single-item source and prepares the player`() {
        underTest.setPlaySource(mock<Uri>(), "123")

        // Preparing before the source is built would prepare the previous item, or nothing at all.
        val inOrder = inOrder(mediaPlayerManager)
        inOrder.verify(mediaPlayerManager)
            .buildPlaySources(argThat { mediaItems.singleOrNull()?.mediaId == "123" })
        inOrder.verify(mediaPlayerManager).playerPrepare()
    }

    @Test
    fun `test that play starts playback`() {
        underTest.play()

        verify(mediaPlayerManager).setPlayWhenReady(true)
    }

    @Test
    fun `test that attachSurface binds the texture view to the player`() {
        val textureView = mock<TextureView>()

        underTest.attachSurface(textureView)

        verify(player).setVideoTextureView(textureView)
    }

    @Test
    fun `test that clearSurface detaches the video output from the player`() {
        underTest.clearSurface()

        verify(player).clearVideoSurface()
    }

    @Test
    fun `test that release detaches the video surface before releasing the player`() {
        underTest.release()

        val inOrder = inOrder(player, mediaPlayerManager)
        inOrder.verify(player).clearVideoSurface()
        inOrder.verify(mediaPlayerManager).release()
    }

    @Test
    fun `test that release is idempotent`() {
        underTest.release()
        clearInvocations(player, mediaPlayerManager)

        underTest.release()

        verify(player, never()).clearVideoSurface()
        verify(mediaPlayerManager, never()).release()
    }

    private fun playbackStates(): Stream<Arguments> = Stream.of(
        Arguments.of(Player.STATE_BUFFERING, true, false, false),
        Arguments.of(Player.STATE_ENDED, false, true, false),
        Arguments.of(Player.STATE_READY, false, false, true),
        Arguments.of(Player.STATE_IDLE, false, false, false),
    )
}
