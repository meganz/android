package mega.privacy.android.app.presentation.videoplayer

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaPlayerManagerTest {
    private val mediaPlayerGateway = mock<MediaPlayerGateway>()
    private lateinit var underTest: MediaPlayerManager

    private val noOpMetadata: (String?, String?, String?) -> Unit = { _, _, _ -> }
    private val noOpTransition: (String?, Boolean) -> Unit = { _, _ -> }
    private val noOpRepeat: (RepeatToggleMode) -> Unit = {}
    private val noOpPlayWhenReady: (MediaPlaybackState, Boolean) -> Unit = { _, _ -> }
    private val noOpPlaybackState: (Int) -> Unit = {}
    private val noOpPlayerError: (Int) -> Unit = {}
    private val noOpVideoSize: (VideoSize) -> Unit = {}

    @BeforeEach
    fun setUp() {
        underTest = MediaPlayerManager(mediaPlayerGateway)
    }

    @AfterEach
    fun tearDown() {
        reset(mediaPlayerGateway)
    }

    /**
     * Invokes [createPlayer] with the given (defaulted) callbacks and returns the
     * [MediaPlayerCallback] the implementation passed to the gateway, so its forwarding can be
     * exercised.
     */
    private fun captureMediaPlayerCallback(
        onMediaItemTransition: (String?, Boolean) -> Unit = noOpTransition,
        onRepeatModeChanged: (RepeatToggleMode) -> Unit = noOpRepeat,
        onPlayWhenReadyChanged: (MediaPlaybackState, Boolean) -> Unit = noOpPlayWhenReady,
        onPlaybackStateChanged: (Int) -> Unit = noOpPlaybackState,
        onPlayerError: (Int) -> Unit = noOpPlayerError,
        onVideoSizeChanged: (VideoSize) -> Unit = noOpVideoSize,
    ): MediaPlayerCallback {
        underTest.createPlayer(
            onMetadataChanged = noOpMetadata,
            onMediaItemTransition = onMediaItemTransition,
            onRepeatModeChanged = onRepeatModeChanged,
            onPlayWhenReadyChanged = onPlayWhenReadyChanged,
            onPlaybackStateChanged = onPlaybackStateChanged,
            onPlayerError = onPlayerError,
            onVideoSizeChanged = onVideoSizeChanged,
        )
        val captor = argumentCaptor<MediaPlayerCallback>()
        verify(mediaPlayerGateway).createPlayer(
            anyOrNull(), anyOrNull(), any(), any(), captor.capture()
        )
        return captor.firstValue
    }

    @Test
    fun `test that createPlayer returns the player created by the gateway`() {
        val player = mock<ExoPlayer>()
        whenever(
            mediaPlayerGateway.createPlayer(anyOrNull(), anyOrNull(), any(), any(), any())
        ).thenReturn(player)

        val result = underTest.createPlayer(
            noOpMetadata, noOpTransition, noOpRepeat, noOpPlayWhenReady,
            noOpPlaybackState, noOpPlayerError, noOpVideoSize,
        )

        assertThat(result).isEqualTo(player)
    }

    @Test
    fun `test that createPlayer requests the gateway with repeat none`() {
        underTest.createPlayer(
            noOpMetadata, noOpTransition, noOpRepeat, noOpPlayWhenReady,
            noOpPlaybackState, noOpPlayerError, noOpVideoSize,
        )
        verify(mediaPlayerGateway).createPlayer(
            anyOrNull(), anyOrNull(), eq(RepeatToggleMode.REPEAT_NONE), any(), any()
        )
    }

    @Test
    fun `test that name change callback forwards to onMetadataChanged`() {
        var received: Triple<String?, String?, String?>? = null
        underTest.createPlayer(
            onMetadataChanged = { title, artist, album -> received = Triple(title, artist, album) },
            onMediaItemTransition = noOpTransition,
            onRepeatModeChanged = noOpRepeat,
            onPlayWhenReadyChanged = noOpPlayWhenReady,
            onPlaybackStateChanged = noOpPlaybackState,
            onPlayerError = noOpPlayerError,
            onVideoSizeChanged = noOpVideoSize,
        )
        val nameCaptor = argumentCaptor<(String?, String?, String?) -> Unit>()
        verify(mediaPlayerGateway).createPlayer(
            anyOrNull(), anyOrNull(), any(), nameCaptor.capture(), any()
        )

        nameCaptor.firstValue.invoke("title", "artist", "album")

        assertThat(received).isEqualTo(Triple("title", "artist", "album"))
    }

    @Test
    fun `test that onMediaItemTransitionCallback forwards handle and update flag`() {
        var received: Pair<String?, Boolean>? = null
        val callback = captureMediaPlayerCallback(
            onMediaItemTransition = { handle, isUpdate -> received = handle to isUpdate }
        )

        callback.onMediaItemTransitionCallback("handle", true)

        assertThat(received).isEqualTo("handle" to true)
    }

    @Test
    fun `test that onRepeatModeChangedCallback forwards repeat mode`() {
        var received: RepeatToggleMode? = null
        val callback = captureMediaPlayerCallback(onRepeatModeChanged = { received = it })

        callback.onRepeatModeChangedCallback(RepeatToggleMode.REPEAT_ONE)

        assertThat(received).isEqualTo(RepeatToggleMode.REPEAT_ONE)
    }

    @Test
    fun `test that onPlayWhenReadyChangedCallback maps to Playing and not paused when playWhenReady is true`() {
        var received: Pair<MediaPlaybackState, Boolean>? = null
        val callback = captureMediaPlayerCallback(onPlayWhenReadyChanged = { s, p -> received = s to p })

        callback.onPlayWhenReadyChangedCallback(
            playWhenReady = true,
            reason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )

        assertThat(received).isEqualTo(MediaPlaybackState.Playing to false)
    }

    @Test
    fun `test that onPlayWhenReadyChangedCallback maps to Paused and paused by user when user requested`() {
        var received: Pair<MediaPlaybackState, Boolean>? = null
        val callback = captureMediaPlayerCallback(onPlayWhenReadyChanged = { s, p -> received = s to p })

        callback.onPlayWhenReadyChangedCallback(
            playWhenReady = false,
            reason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )

        assertThat(received).isEqualTo(MediaPlaybackState.Paused to true)
    }

    @Test
    fun `test that onPlayWhenReadyChangedCallback maps to Paused and not paused by user when reason is not user request`() {
        var received: Pair<MediaPlaybackState, Boolean>? = null
        val callback = captureMediaPlayerCallback(onPlayWhenReadyChanged = { s, p -> received = s to p })

        callback.onPlayWhenReadyChangedCallback(
            playWhenReady = false,
            reason = Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
        )

        assertThat(received).isEqualTo(MediaPlaybackState.Paused to false)
    }

    @Test
    fun `test that onPlaybackStateChangedCallback forwards state`() {
        var received: Int? = null
        val callback = captureMediaPlayerCallback(onPlaybackStateChanged = { received = it })

        callback.onPlaybackStateChangedCallback(Player.STATE_READY)

        assertThat(received).isEqualTo(Player.STATE_READY)
    }

    @Test
    fun `test that onPlayerErrorCallback forwards error code`() {
        var received: Int? = null
        val callback = captureMediaPlayerCallback(onPlayerError = { received = it })

        callback.onPlayerErrorCallback(42)

        assertThat(received).isEqualTo(42)
    }

    @Test
    fun `test that onVideoSizeCallback forwards non-zero size`() {
        var received: VideoSize? = null
        val callback = captureMediaPlayerCallback(onVideoSizeChanged = { received = it })

        callback.onVideoSizeCallback(1920, 1080)

        assertThat(received).isEqualTo(VideoSize(1920, 1080))
    }

    @Test
    fun `test that onVideoSizeCallback ignores zero width`() {
        var called = false
        val callback = captureMediaPlayerCallback(onVideoSizeChanged = { called = true })

        callback.onVideoSizeCallback(0, 1080)

        assertThat(called).isFalse()
    }

    @Test
    fun `test that onVideoSizeCallback ignores zero height`() {
        var called = false
        val callback = captureMediaPlayerCallback(onVideoSizeChanged = { called = true })

        callback.onVideoSizeCallback(1920, 0)

        assertThat(called).isFalse()
    }

    @Test
    fun `test that retry calls gateway media player retry`() {
        underTest.retry()
        verify(mediaPlayerGateway).mediaPlayerRetry(true)
    }

    @Test
    fun `test that release stops and releases the player`() {
        underTest.release()
        verify(mediaPlayerGateway).playerStop()
        verify(mediaPlayerGateway).playerRelease()
    }

    @Test
    fun `test that getCurrentMediaItem returns the gateway value`() {
        val item = MediaItem.Builder().setMediaId("1").build()
        whenever(mediaPlayerGateway.getCurrentMediaItem()).thenReturn(item)
        assertThat(underTest.getCurrentMediaItem()).isEqualTo(item)
    }

    @Test
    fun `test that getCurrentItemDuration returns the gateway value`() {
        whenever(mediaPlayerGateway.getCurrentItemDuration()).thenReturn(123L)
        assertThat(underTest.getCurrentItemDuration()).isEqualTo(123L)
    }

    @Test
    fun `test that getCurrentPlayingPosition returns the gateway value`() {
        whenever(mediaPlayerGateway.getCurrentPlayingPosition()).thenReturn(456L)
        assertThat(underTest.getCurrentPlayingPosition()).isEqualTo(456L)
    }

    @Test
    fun `test that setPlayWhenReady delegates to gateway`() {
        underTest.setPlayWhenReady(true)
        verify(mediaPlayerGateway).setPlayWhenReady(true)
    }

    @Test
    fun `test that getPlayWhenReady returns the gateway value`() {
        whenever(mediaPlayerGateway.getPlayWhenReady()).thenReturn(true)
        assertThat(underTest.getPlayWhenReady()).isTrue()
    }

    @Test
    fun `test that playerSeekTo delegates to gateway`() {
        underTest.playerSeekTo(3)
        verify(mediaPlayerGateway).playerSeekTo(3)
    }

    @Test
    fun `test that playerSeekToPositionInMs delegates to gateway`() {
        underTest.playerSeekToPositionInMs(789L)
        verify(mediaPlayerGateway).playerSeekToPositionInMs(789L)
    }

    @Test
    fun `test that buildPlaySources delegates to gateway`() {
        val sources = MediaPlaySources(
            mediaItems = emptyList(),
            newIndexForCurrentItem = 0,
            nameToDisplay = null,
        )
        underTest.buildPlaySources(sources)
        verify(mediaPlayerGateway).buildPlaySources(sources)
    }

    @Test
    fun `test that playerPrepare delegates to gateway`() {
        underTest.playerPrepare()
        verify(mediaPlayerGateway).playerPrepare()
    }

    @Test
    fun `test that setRepeatToggleMode delegates to gateway`() {
        underTest.setRepeatToggleMode(RepeatToggleMode.REPEAT_ONE)
        verify(mediaPlayerGateway).setRepeatToggleMode(RepeatToggleMode.REPEAT_ONE)
    }

    @Test
    fun `test that mediaPlayerIsPlaying returns the gateway value`() {
        whenever(mediaPlayerGateway.mediaPlayerIsPlaying()).thenReturn(true)
        assertThat(underTest.mediaPlayerIsPlaying()).isTrue()
    }

    @Test
    fun `test that updatePlaybackSpeed delegates to gateway`() {
        val item = mock<SpeedPlaybackItem>()
        underTest.updatePlaybackSpeed(item)
        verify(mediaPlayerGateway).updatePlaybackSpeed(item)
    }

    @Test
    fun `test that addSubtitle delegates to gateway and returns the result`() {
        whenever(mediaPlayerGateway.addSubtitle("url")).thenReturn(true)
        assertThat(underTest.addSubtitle("url")).isTrue()
        verify(mediaPlayerGateway).addSubtitle("url")
    }

    @Test
    fun `test that showSubtitle delegates to gateway`() {
        underTest.showSubtitle()
        verify(mediaPlayerGateway).showSubtitle()
    }

    @Test
    fun `test that hideSubtitle delegates to gateway`() {
        underTest.hideSubtitle()
        verify(mediaPlayerGateway).hideSubtitle()
    }
}
