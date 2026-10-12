package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.net.Uri
import android.view.TextureView
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.model.VideoControllerState
import mega.privacy.android.feature.mediaplayer.videoplayer.navigation.VideoPlayerLaunchSourceHolder
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSource
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoPlayerViewModelTest {

    private val launchSourceHolder = mock<VideoPlayerLaunchSourceHolder>()
    private val gateway = mock<VideoMediaControllerGateway>()
    private val textureView = mock<TextureView>()
    private val httpServerStopUseCase = mock<HttpServerStopUseCase>()

    private lateinit var playerState: MutableStateFlow<VideoControllerState>
    private lateinit var underTest: VideoPlayerViewModel

    @BeforeEach
    fun setUp() {
        reset(launchSourceHolder, gateway, textureView, httpServerStopUseCase)
        playerState = MutableStateFlow(VideoControllerState())
        whenever(gateway.playerState).thenReturn(playerState)
    }

    private fun TestScope.initViewModel() {
        underTest = VideoPlayerViewModel(
            launchId = LAUNCH_ID,
            launchSourceHolder = launchSourceHolder,
            gateway = gateway,
            httpServerStopUseCase = httpServerStopUseCase,
            applicationScope = this,
        )
    }

    /**
     * How many items arrive, and which, depends on how the player state was seeded and on what
     * the state flow conflates away before the first `awaitItem`, so a test that changes
     * `playWhenReady` waits for the value it cares about rather than counting items. Reaching the
     * matching item is itself the assertion; it is returned for tests that also check the rest of
     * the state that arrived with it.
     */
    private suspend fun ReceiveTurbine<VideoPlayerUiState>.awaitUntilPlayWhenReady(
        expected: Boolean,
    ): VideoPlayerUiState.Data = awaitUntilData { it.playWhenReady == expected }

    /** The general form of [awaitUntilPlayWhenReady], for the other fields a test waits on. */
    private suspend fun ReceiveTurbine<VideoPlayerUiState>.awaitUntilData(
        predicate: (VideoPlayerUiState.Data) -> Boolean,
    ): VideoPlayerUiState.Data {
        while (true) {
            val item = awaitItem()
            if (item is VideoPlayerUiState.Data && predicate(item)) return item
        }
    }

    /** Clearing the store is what calls the ViewModel's protected `onCleared`. */
    private fun clearViewModel() {
        ViewModelStore().apply { put("underTest", underTest) }.clear()
    }

    private fun launchSource(
        contentUri: Uri? = mock(),
        handle: Long = TEST_HANDLE,
        fileName: String? = TEST_FILE_NAME,
        sourceType: VideoPlayerLaunchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
        needStopHttpServer: Boolean = false,
    ) = VideoPlayerLaunchSource(
        contentUri = contentUri,
        handle = handle,
        fileName = fileName,
        sourceType = sourceType,
        needStopHttpServer = needStopHttpServer,
    )

    private suspend fun assertRejected() {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(VideoPlayerUiState.InvalidLaunchSource)
        }
    }

    @Test
    fun `test that uiState emits Data when the holder returns a launch source`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(VideoPlayerUiState.Data::class.java)
            with(state as VideoPlayerUiState.Data) {
                assertThat(currentPlayingHandle).isEqualTo(TEST_HANDLE)
                assertThat(currentPlayingItemName).isEqualTo(TEST_FILE_NAME)
                assertThat(launchSourceType).isEqualTo(VideoPlayerLaunchSourceType.VideoBrowse)
            }
        }
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the holder returns nothing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(null)
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(VideoPlayerUiState.InvalidLaunchSource)
        }
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the launch source type is Invalid`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID))
                .thenReturn(launchSource(sourceType = VideoPlayerLaunchSourceType.Invalid))
            initViewModel()

            assertRejected()
        }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the content URI is missing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource(contentUri = null))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the node handle is invalid`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID))
            .thenReturn(launchSource(handle = INVALID_HANDLE))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the file name is missing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource(fileName = null))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits Data when the launch source type is Unknown`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID))
            .thenReturn(launchSource(sourceType = VideoPlayerLaunchSourceType.Unknown))
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideoPlayerUiState.Data::class.java)
        }
    }

    @Test
    fun `test that the launch source is consumed with the id the ViewModel was created with`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
            initViewModel()

            underTest.uiState.test { awaitItem() }

            verify(launchSourceHolder).consume(LAUNCH_ID)
        }

    @Test
    fun `test that the launch source is consumed once however often the state is collected`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
            initViewModel()

            underTest.uiState.test { awaitItem() }
            underTest.uiState.test { awaitItem() }

            // The holder hands a source out once, so a second read would come back empty and the
            // player would close itself mid-session.
            verify(launchSourceHolder).consume(LAUNCH_ID)
        }

    @Test
    fun `test that onSurfaceReady attaches the surface`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.onSurfaceReady(textureView)

        verify(gateway).attachSurface(textureView)
    }

    @Test
    fun `test that onSurfaceReady plays the launched video`() = runTest {
        val uri = mock<Uri>()
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource(contentUri = uri))
        initViewModel()

        underTest.onSurfaceReady(textureView)

        verify(gateway).setPlaySource(uri, TEST_HANDLE.toString())
        verify(gateway).play()
    }

    @Test
    fun `test that onSurfaceReady does not load the video again when the surface is recreated`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
            initViewModel()

            underTest.onSurfaceReady(textureView)
            underTest.onSurfaceReady(textureView)

            // A configuration change recreates the surface. Loading again would restart the video
            // from the beginning.
            verify(gateway, times(1)).setPlaySource(any(), any())
            verify(gateway, times(1)).play()
            verify(gateway, times(2)).attachSurface(textureView)
        }

    @Test
    fun `test that onSurfaceReady tries again when starting playback failed`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        whenever(gateway.setPlaySource(any(), any()))
            .thenThrow(IllegalStateException("player rejected the item"))
            .thenAnswer { }
        initViewModel()

        underTest.onSurfaceReady(textureView)
        underTest.onSurfaceReady(textureView)

        // Latching the flag on the failed attempt would strand the screen on black with no way
        // back, since nothing retries other than a new surface.
        verify(gateway, times(2)).setPlaySource(any(), any())
        verify(gateway, times(1)).play()
    }

    @Test
    fun `test that onSurfaceReady does not play when the launch source is unusable`() = runTest {
        // A rejected launch can still carry a content URI, so the decision has to come from the
        // resolved state rather than from the URI being present.
        whenever(launchSourceHolder.consume(LAUNCH_ID))
            .thenReturn(launchSource(handle = INVALID_HANDLE))
        initViewModel()

        underTest.onSurfaceReady(textureView)

        verify(gateway, never()).setPlaySource(any(), any())
        verify(gateway, never()).play()
    }

    @Test
    fun `test that onSurfaceReleased clears the surface`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.onSurfaceReleased()

        verify(gateway).clearSurface()
    }

    @Test
    fun `test that onPlayPauseClick pauses when playback is running`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()
        playerState.value = VideoControllerState(playWhenReady = true)

        // Clicked while the state is collected: the ViewModel reads the value the button was
        // drawn from, which only stands while something is subscribed.
        underTest.uiState.test {
            awaitUntilPlayWhenReady(true)
            underTest.onPlayPauseClick()
        }

        verify(gateway).pause()
        verify(gateway, never()).play()
    }

    @Test
    fun `test that onPlayPauseClick plays when playback is paused`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()
        playerState.value = VideoControllerState(playWhenReady = false)

        underTest.uiState.test {
            awaitUntilPlayWhenReady(false)
            underTest.onPlayPauseClick()
        }

        verify(gateway).play()
        verify(gateway, never()).pause()
    }

    @Test
    fun `test that onPlayPauseClick does nothing when the launch source is unusable`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(null)
        initViewModel()

        underTest.uiState.test {
            while (awaitItem() !is VideoPlayerUiState.InvalidLaunchSource) Unit
            underTest.onPlayPauseClick()
        }

        verify(gateway, never()).play()
        verify(gateway, never()).pause()
    }

    @Test
    fun `test that onSeekTo moves the player to the position`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()
        playerState.value = VideoControllerState(durationMs = TEST_DURATION_MS)

        underTest.uiState.test {
            awaitUntilData { it.durationMs == TEST_DURATION_MS }
            underTest.onSeekTo(TEST_POSITION_MS)
        }

        verify(gateway).seekTo(TEST_POSITION_MS)
    }

    @Test
    fun `test that onSeekTo clamps a position past the end of the item`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()
        playerState.value = VideoControllerState(durationMs = TEST_DURATION_MS)

        underTest.uiState.test {
            awaitUntilData { it.durationMs == TEST_DURATION_MS }
            underTest.onSeekTo(TEST_DURATION_MS * 4)
        }

        verify(gateway).seekTo(TEST_DURATION_MS)
    }

    @Test
    fun `test that onSeekTo does nothing before the player reports a duration`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        // Seeking within an item of unknown length would land somewhere arbitrary; the bar has
        // nothing to drag along yet either.
        underTest.uiState.test {
            awaitUntilData { it.durationMs == 0L }
            underTest.onSeekTo(TEST_POSITION_MS)
        }

        verify(gateway, never()).seekTo(any())
    }

    @Test
    fun `test that onSeekTo does nothing when the launch source is unusable`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(null)
        initViewModel()

        underTest.uiState.test {
            while (awaitItem() !is VideoPlayerUiState.InvalidLaunchSource) Unit
            underTest.onSeekTo(TEST_POSITION_MS)
        }

        verify(gateway, never()).seekTo(any())
    }

    @Test
    fun `test that the transport controls reach the player once the launch resolves`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            awaitUntilData { true }
            underTest.onRewindClick()
            underTest.onFastForwardClick()
            underTest.onPreviousClick()
            underTest.onNextClick()
        }

        // Forwarded untouched: the increment and the clamping belong to the player, and so does
        // the decision about what previous means when there is no queue behind it.
        verify(gateway).seekBack()
        verify(gateway).seekForward()
        verify(gateway).skipToPrevious()
        verify(gateway).skipToNext()
    }

    @Test
    fun `test that the transport controls do nothing when the launch source is unusable`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(null)
            initViewModel()

            underTest.uiState.test {
                while (awaitItem() !is VideoPlayerUiState.InvalidLaunchSource) Unit
                underTest.onRewindClick()
                underTest.onFastForwardClick()
                underTest.onPreviousClick()
                underTest.onNextClick()
            }

            verify(gateway, never()).seekBack()
            verify(gateway, never()).seekForward()
            verify(gateway, never()).skipToPrevious()
            verify(gateway, never()).skipToNext()
        }

    @Test
    fun `test that uiState carries the position and duration the player reports`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            playerState.value = VideoControllerState(
                currentPositionMs = TEST_POSITION_MS,
                durationMs = TEST_DURATION_MS,
            )

            with(awaitUntilData { it.durationMs == TEST_DURATION_MS }) {
                assertThat(currentPositionMs).isEqualTo(TEST_POSITION_MS)
            }
        }
    }

    @Test
    fun `test that uiState carries whether the user has playback running`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            awaitUntilPlayWhenReady(expected = false)

            playerState.value = VideoControllerState(playWhenReady = true)

            awaitUntilPlayWhenReady(expected = true)
        }
    }

    @Test
    fun `test that uiState keeps playWhenReady set while the player rebuffers`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            awaitUntilPlayWhenReady(expected = false)

            // The button has to keep saying "pause" through a rebuffer: the user never asked for
            // playback to stop, so a tap must still mean pause.
            playerState.value = VideoControllerState(playWhenReady = true, isBuffering = true)

            assertThat(awaitUntilPlayWhenReady(expected = true).isLoading).isTrue()
        }
    }

    @Test
    fun `test that uiState carries the video size the player reports`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            assertThat((awaitItem() as VideoPlayerUiState.Data).videoWidth).isEqualTo(0)

            playerState.value = VideoControllerState(videoWidth = 1920, videoHeight = 1080)

            with(awaitItem() as VideoPlayerUiState.Data) {
                assertThat(videoWidth).isEqualTo(1920)
                assertThat(videoHeight).isEqualTo(1080)
            }
        }
    }

    @Test
    fun `test that uiState is loading until the player is ready`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            // Idle: prepared but no frame decoded yet, which is what the spinner covers.
            assertThat((awaitItem() as VideoPlayerUiState.Data).isLoading).isTrue()

            playerState.value = VideoControllerState(isReady = true)
            assertThat((awaitItem() as VideoPlayerUiState.Data).isLoading).isFalse()

            // Re-buffering mid-playback puts it back.
            playerState.value = VideoControllerState(isBuffering = true)
            assertThat((awaitItem() as VideoPlayerUiState.Data).isLoading).isTrue()
        }
    }

    @Test
    fun `test that uiState is not loading once the item has ended`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            awaitItem()

            // Ended leaves isReady false as well, so a plain negation would spin forever on the
            // last frame.
            playerState.value = VideoControllerState(isEnded = true)

            assertThat((awaitItem() as VideoPlayerUiState.Data).isLoading).isFalse()
        }
    }

    @Test
    fun `test that the player is released when the ViewModel is cleared`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        clearViewModel()

        verify(gateway).release()
    }

    @Test
    fun `test that the streaming server is stopped when the launch handed its ownership over`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID))
                .thenReturn(launchSource(needStopHttpServer = true))
            initViewModel()

            clearViewModel()
            advanceUntilIdle()

            verify(httpServerStopUseCase).invoke()
        }

    @Test
    fun `test that the streaming server is left running when the launch did not own it`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID))
                .thenReturn(launchSource(needStopHttpServer = false))
            initViewModel()

            clearViewModel()
            advanceUntilIdle()

            // Something else started it and is still using it — a local file needs no server at
            // all, and stopping one we were not given would break whoever did start it.
            verify(httpServerStopUseCase, never()).invoke()
        }

    private companion object {
        const val LAUNCH_ID = "launch-id"
        const val TEST_HANDLE = 123456L
        const val TEST_FILE_NAME = "video.mp4"
        const val TEST_DURATION_MS = 225_000L
        const val TEST_POSITION_MS = 72_000L
    }
}
