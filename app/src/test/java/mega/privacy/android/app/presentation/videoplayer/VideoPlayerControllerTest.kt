package mega.privacy.android.app.presentation.videoplayer

import android.os.Build
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerOverlayChipState
import mega.privacy.mobile.analytics.event.VideoPlayerBrightnessSwipeEvent
import mega.privacy.mobile.analytics.event.VideoPlayerDoubleTapSeekBackwardEvent
import mega.privacy.mobile.analytics.event.VideoPlayerDoubleTapSeekForwardEvent
import mega.privacy.mobile.analytics.event.VideoPlayerFullScreenPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerLongPressSpeedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerOriginalPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerPinchToZoomEvent
import mega.privacy.mobile.analytics.event.VideoPlayerVolumeSwipeEvent
import mega.privacy.mobile.analytics.event.VideoPlayerZoomToFillEvent
import mega.privacy.mobile.analytics.event.VideoPlayerZoomToFitEvent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.Q])
class VideoPlayerControllerTest {

    private val mockPlayerView = mock<PlayerView>()
    private val mockPlayer = mock<Player>()
    private val mockAnalyticsTracker = mock<AnalyticsTracker>()
    private val mockRepeatToggle = mock<ImageButton>()
    private val mockMoreOption = mock<ImageButton>()
    private val mockFullscreen = mock<ImageButton>()
    private val mockControllerView = mock<View>()
    private val mockUnlockView = mock<View>()
    private val mockUnlockButton = mock<ImageButton>()
    private val mockSpeedPlayback = mock<TextView>()
    private val mockDeviceRotate = mock<ImageButton>()
    private val mockRew = mock<ImageButton>()
    private val mockFfwd = mock<ImageButton>()

    private val onLongPressSpeedChange = mock<(SpeedPlaybackItem) -> Unit>()
    private val onLongPressActivated = mock<() -> Unit>()
    private val fullscreenClickedCallback = mock<(Boolean) -> Unit>()
    private val playerViewClicked = mock<() -> Unit>()
    private val onBrightnessChange = mock<(Float) -> Unit>()
    private val onVolumeChange = mock<(Float) -> Unit>()

    private lateinit var activity: AppCompatActivity
    private lateinit var mockContainer: FrameLayout
    private var controller: VideoPlayerController? = null

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        mockContainer = mock()
        whenever(mockContainer.findViewById<ImageButton>(R.id.repeat_toggle)).thenReturn(
            mockRepeatToggle
        )
        whenever(mockContainer.findViewById<PlayerView>(R.id.player_compose_view)).thenReturn(
            mockPlayerView
        )
        whenever(mockContainer.findViewById<ImageButton>(R.id.more_option)).thenReturn(
            mockMoreOption
        )
        whenever(mockContainer.findViewById<ImageButton>(R.id.full_screen)).thenReturn(
            mockFullscreen
        )
        whenever(mockContainer.findViewById<View>(R.id.layout_player)).thenReturn(mockControllerView)
        whenever(mockContainer.findViewById<View>(R.id.layout_unlock)).thenReturn(mockUnlockView)
        whenever(mockContainer.findViewById<ImageButton>(R.id.image_button_unlock)).thenReturn(
            mockUnlockButton
        )
        whenever(mockContainer.findViewById<TextView>(R.id.speed_playback)).thenReturn(
            mockSpeedPlayback
        )
        whenever(mockContainer.findViewById<ImageButton>(R.id.device_rotated)).thenReturn(
            mockDeviceRotate
        )
        whenever(mockContainer.findViewById<ImageButton>(R.id.exo_rew)).thenReturn(mockRew)
        whenever(mockContainer.findViewById<ImageButton>(R.id.exo_ffwd)).thenReturn(mockFfwd)
        whenever(mockPlayerView.width).thenReturn(1000)
        whenever(mockPlayerView.height).thenReturn(1000)
        whenever(mockPlayer.isPlaying).thenReturn(true)
        whenever(mockPlayerView.player).thenReturn(mockPlayer)
        Analytics.initialise(mockAnalyticsTracker)
    }

    @After
    fun tearDown() {
        controller?.release()
        controller = null
        Analytics.initialise(null as AnalyticsTracker?)
    }

    private fun createController(
        currentSpeedPlayback: SpeedPlaybackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1X,
        isGesturesEnabled: Boolean = true,
        isFullscreen: Boolean = false,
        isLocked: Boolean = false,
    ) = VideoPlayerController(
        context = activity,
        uiState = VideoPlayerUiState(
            currentSpeedPlayback = currentSpeedPlayback,
            isGesturesEnabled = isGesturesEnabled,
            isFullscreen = isFullscreen,
            isLocked = isLocked,
        ),
        container = mockContainer,
        updateRepeatToggleMode = {},
        updateIsVideoOptionPopupShown = {},
        updateIsSpeedOptionsShown = {},
        updateLockStatus = {},
        fullscreenClickedCallback = fullscreenClickedCallback,
        lockStateChanged = {},
        playerViewClicked = playerViewClicked,
        onSnapshotSelected = {},
        resetAutoHideTimer = {},
        onLongPressSpeedChange = onLongPressSpeedChange,
        onLongPressActivated = onLongPressActivated,
        onBrightnessChange = onBrightnessChange,
        onVolumeChange = onVolumeChange,
    ).also { controller = it }

    private fun VideoPlayerController.runStartLongPressRunnable() {
        val field = VideoPlayerController::class.java.getDeclaredField("startLongPressRunnable")
        field.isAccessible = true
        (field.get(this) as Runnable).run()
    }

    private fun VideoPlayerController.callReleaseLongPress() {
        val method = VideoPlayerController::class.java.getDeclaredMethod("releaseLongPress")
        method.isAccessible = true
        method.invoke(this)
    }

    private fun VideoPlayerController.zoomState(): VideoPlayerZoomState {
        val field = VideoPlayerController::class.java.getDeclaredField("zoomState")
        field.isAccessible = true
        return field.get(this) as VideoPlayerZoomState
    }

    private fun VideoPlayerController.setZoomLevel(level: Float) {
        val field = VideoPlayerZoomState::class.java.getDeclaredField("zoomLevel")
        field.isAccessible = true
        field.setFloat(zoomState(), level)
    }

    private fun VideoPlayerController.getZoomLevel(): Float = zoomState().zoomLevel

    private fun VideoPlayerController.getLegacyFloatField(name: String): Float {
        val field = VideoPlayerController::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.getFloat(this)
    }

    private fun VideoPlayerController.setLegacyZoomLevel(level: Float) {
        val field = VideoPlayerController::class.java.getDeclaredField("legacyZoomLevel")
        field.isAccessible = true
        field.setFloat(this, level)
    }

    private fun VideoPlayerController.getLegacyZoomLevel(): Float =
        getLegacyFloatField("legacyZoomLevel")

    private fun VideoPlayerController.getLegacyTranslationX(): Float =
        getLegacyFloatField("legacyTranslationX")

    private fun VideoPlayerController.getLegacyTranslationY(): Float =
        getLegacyFloatField("legacyTranslationY")

    private fun VideoPlayerController.getTranslationX(): Float = zoomState().translationX

    private fun VideoPlayerController.getTranslationY(): Float = zoomState().translationY

    private fun VideoPlayerController.callOnScroll(
        distanceX: Float = 50f,
        distanceY: Float = 50f,
        startX: Float? = null,
        pointerCount: Int = 1,
    ) {
        val gdField = VideoPlayerController::class.java.getDeclaredField("gestureDetector")
        gdField.isAccessible = true
        val gd = gdField.get(this) as? GestureDetector ?: return
        val listenerField = GestureDetector::class.java.getDeclaredField("mListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(gd) as? GestureDetector.OnGestureListener ?: return
        val e1 = startX?.let { x ->
            MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, 100f, 0)
        }
        val e2 = if (pointerCount == 1) {
            MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 100f, 100f, 0)
        } else {
            val properties = Array(pointerCount) { i ->
                MotionEvent.PointerProperties()
                    .also { it.id = i; it.toolType = MotionEvent.TOOL_TYPE_FINGER }
            }
            val coords = Array(pointerCount) { i ->
                MotionEvent.PointerCoords().also { it.x = 100f + i * 50f; it.y = 100f }
            }
            MotionEvent.obtain(
                0L,
                0L,
                MotionEvent.ACTION_MOVE,
                pointerCount,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0
            )
        }
        try {
            listener.onScroll(e1, e2, distanceX, distanceY)
        } finally {
            e1?.recycle()
            e2.recycle()
        }
    }

    private fun VideoPlayerController.callOnSingleTapConfirmed() {
        val gdField = VideoPlayerController::class.java.getDeclaredField("gestureDetector")
        gdField.isAccessible = true
        val gd = gdField.get(this) as? GestureDetector ?: return
        val listenerField = GestureDetector::class.java.getDeclaredField("mDoubleTapListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(gd) as? GestureDetector.OnDoubleTapListener ?: return
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 0f, 0f, 0)
        try {
            listener.onSingleTapConfirmed(event)
        } finally {
            event.recycle()
        }
    }

    private fun VideoPlayerController.callOnDoubleTap(x: Float = 500f): Boolean {
        val gdField = VideoPlayerController::class.java.getDeclaredField("gestureDetector")
        gdField.isAccessible = true
        val gd = gdField.get(this) as? GestureDetector ?: return false
        val listenerField = GestureDetector::class.java.getDeclaredField("mDoubleTapListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(gd) as? GestureDetector.OnDoubleTapListener ?: return false
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, 500f, 0)
        return try {
            listener.onDoubleTap(event)
        } finally {
            event.recycle()
        }
    }

    private fun VideoPlayerController.getZoomChipState(): VideoPlayerOverlayChipState? {
        val field = VideoPlayerController::class.java.getDeclaredField("zoomChipState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (field.get(this) as MutableState<VideoPlayerOverlayChipState?>).value
    }

    private fun VideoPlayerController.scaleListener(): ScaleGestureDetector.OnScaleGestureListener {
        val sgdField = VideoPlayerController::class.java.getDeclaredField("scaleGestureDetector")
        sgdField.isAccessible = true
        val sgd = sgdField.get(this) as ScaleGestureDetector
        val listenerField = ScaleGestureDetector::class.java.getDeclaredField("mListener")
        listenerField.isAccessible = true
        return listenerField.get(sgd) as ScaleGestureDetector.OnScaleGestureListener
    }

    private fun VideoPlayerController.callOnScale(scaleFactor: Float) {
        val mockDetector = mock<ScaleGestureDetector>()
        whenever(mockDetector.scaleFactor).thenReturn(scaleFactor)
        scaleListener().onScale(mockDetector)
    }

    private fun VideoPlayerController.callOnScaleBegin() {
        scaleListener().onScaleBegin(mock())
    }

    private fun VideoPlayerController.callOnScaleEnd() {
        scaleListener().onScaleEnd(mock())
    }

    private fun dispatchTouchEvent(action: Int, x: Float = 400f) {
        val captor = argumentCaptor<View.OnTouchListener>()
        verify(mockPlayerView, atLeastOnce()).setOnTouchListener(captor.capture())
        val event = MotionEvent.obtain(0L, 0L, action, x, 100f, 0)
        try {
            captor.lastValue.onTouch(mockPlayerView, event)
        } finally {
            event.recycle()
        }
    }

    private fun dispatchTouchUp(x: Float = 400f) = dispatchTouchEvent(MotionEvent.ACTION_UP, x)

    private fun dispatchTouchCancel() = dispatchTouchEvent(MotionEvent.ACTION_CANCEL)

    @Test
    fun `test that startLongPressRunnable calls onLongPressActivated when current speed is not 2x`() {
        val controller = createController()
        controller.runStartLongPressRunnable()
        verify(onLongPressActivated).invoke()
    }

    @Test
    fun `test that startLongPressRunnable calls onLongPressSpeedChange with 2x when current speed is not 2x`() {
        val controller = createController()
        controller.runStartLongPressRunnable()
        verify(onLongPressSpeedChange).invoke(VideoSpeedPlaybackItem.PlaybackSpeed_2X)
    }

    @Test
    fun `test that startLongPressRunnable does not call onLongPressActivated when current speed is already 2x`() {
        val controller =
            createController(currentSpeedPlayback = VideoSpeedPlaybackItem.PlaybackSpeed_2X)
        controller.runStartLongPressRunnable()
        verify(onLongPressActivated, never()).invoke()
    }

    @Test
    fun `test that startLongPressRunnable does not call onLongPressSpeedChange when current speed is already 2x`() {
        val controller =
            createController(currentSpeedPlayback = VideoSpeedPlaybackItem.PlaybackSpeed_2X)
        controller.runStartLongPressRunnable()
        verify(onLongPressSpeedChange, never()).invoke(any())
    }

    @Test
    fun `test that releaseLongPress calls onLongPressSpeedChange with saved speed`() {
        val controller = createController()
        controller.runStartLongPressRunnable()
        controller.callReleaseLongPress()
        verify(onLongPressSpeedChange).invoke(VideoSpeedPlaybackItem.PlaybackSpeed_1X)
    }

    @Test
    fun `test that releaseLongPress does not call onLongPressSpeedChange when no speed was saved`() {
        val controller = createController()
        controller.callReleaseLongPress()
        verify(onLongPressSpeedChange, never()).invoke(any())
    }

    @Test
    fun `test that release calls onLongPressSpeedChange with saved speed when long press is active`() {
        val controller = createController()
        controller.runStartLongPressRunnable()
        controller.release()
        verify(onLongPressSpeedChange).invoke(VideoSpeedPlaybackItem.PlaybackSpeed_1X)
    }

    @Test
    fun `test that onScroll does not update translations when long press is active`() {
        val controller = createController()
        controller.setZoomLevel(2.0f)
        controller.runStartLongPressRunnable()
        controller.callOnScroll(distanceX = 50f, distanceY = 50f)
        assertThat(controller.getTranslationX()).isEqualTo(0f)
        assertThat(controller.getTranslationY()).isEqualTo(0f)
    }

    @Test
    fun `test that onScroll updates translations when long press is not active and zoomed in`() {
        val controller = createController()
        controller.setZoomLevel(2.0f)
        controller.callOnScroll(distanceX = 50f, distanceY = 30f)
        assertThat(controller.getTranslationX()).isEqualTo(-50f)
        assertThat(controller.getTranslationY()).isEqualTo(-30f)
    }

    @Test
    fun `test that onScroll pans vertically between fit and fill when the height overflows`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.setZoomLevel(1.5f)
        controller.callOnScroll(distanceX = 10f, distanceY = 50f, startX = 400f)
        assertThat(controller.getTranslationY()).isEqualTo(-50f)
        assertThat(controller.getTranslationX()).isEqualTo(0f)
        verify(onBrightnessChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll pans horizontally between fit and fill when the swipe is horizontal`() {
        stubVideoSurfaceView(videoWidth = 1000, videoHeight = 500)
        val controller = createController()
        controller.setZoomLevel(1.5f)
        controller.callOnScroll(distanceX = 50f, distanceY = 10f, startX = 400f)
        assertThat(controller.getTranslationX()).isEqualTo(-50f)
        assertThat(controller.getTranslationY()).isEqualTo(0f)
    }

    @Test
    fun `test that onScroll adjusts brightness when the swipe is vertical and only the width overflows`() {
        stubVideoSurfaceView(videoWidth = 1000, videoHeight = 500)
        val controller = createController()
        controller.setZoomLevel(1.5f)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange).invoke(any())
        assertThat(controller.getTranslationX()).isEqualTo(0f)
    }

    @Test
    fun `test that onSingleTapConfirmed calls playerViewClicked`() {
        val controller = createController()
        controller.callOnSingleTapConfirmed()
        verify(playerViewClicked).invoke()
    }

    @Test
    fun `test that onScale enters fullscreen when not in fullscreen and scale factor is greater than 1`() {
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.1f)
        verify(fullscreenClickedCallback).invoke(true)
    }

    @Test
    fun `test that onScale exits fullscreen when in fullscreen at minimum zoom and scale factor is less than 1`() {
        val controller = createController(isFullscreen = true)
        controller.callOnScale(scaleFactor = 0.9f)
        verify(fullscreenClickedCallback).invoke(false)
    }

    @Test
    fun `test that onScale does not trigger fullscreen callback when gestures are disabled`() {
        val controller = createController(isGesturesEnabled = false)
        controller.callOnScale(scaleFactor = 1.1f)
        verify(fullscreenClickedCallback, never()).invoke(any())
    }

    @Test
    fun `test that onScale applies the legacy zoom when gestures are disabled`() {
        val controller = createController(isGesturesEnabled = false, isFullscreen = true)
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getLegacyZoomLevel()).isGreaterThan(1.0f)
        assertThat(controller.getZoomLevel()).isEqualTo(1.0f)
    }

    @Test
    fun `test that onScale does not trigger fullscreen callback when locked`() {
        val controller = createController(isLocked = true)
        controller.callOnScale(scaleFactor = 1.1f)
        verify(fullscreenClickedCallback, never()).invoke(any())
    }

    @Test
    fun `test that onScale does not trigger fullscreen callback when in fullscreen and scale factor is greater than 1`() {
        val controller = createController(isFullscreen = true)
        controller.callOnScale(scaleFactor = 1.5f)
        verify(fullscreenClickedCallback, never()).invoke(any())
    }

    @Test
    fun `test that onScale updates zoom level when in fullscreen and scale factor is greater than 1`() {
        val controller = createController(isFullscreen = true)
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getZoomLevel()).isGreaterThan(1.0f)
    }

    @Test
    fun `test that onScroll triggers brightness change when gestures enabled and swipe on left half`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll triggers volume change when gestures enabled and swipe on right half`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 600f)
        verify(onVolumeChange).invoke(any())
        verify(onBrightnessChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when gestures disabled`() {
        val controller = createController(isGesturesEnabled = false)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when locked`() {
        val controller = createController(isGesturesEnabled = true, isLocked = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when zoomed in`() {
        val controller = createController(isGesturesEnabled = true)
        controller.setZoomLevel(2.0f)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when e1 is null`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = null)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when swipe is horizontal`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 50f, distanceY = 10f, startX = 400f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when suppressScrollGesture is true`() {
        val controller = createController(isGesturesEnabled = true)
        controller.setSuppressScrollGesture(true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll does not trigger brightness or volume change when pointer count is greater than 1`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f, pointerCount = 2)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that onScroll triggers brightness change on consecutive scroll events in same gesture`() {
        val controller = createController(isGesturesEnabled = true)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        verify(onBrightnessChange, times(2)).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    private fun VideoPlayerController.setSuppressScrollGesture(suppress: Boolean) {
        val field = VideoPlayerController::class.java.getDeclaredField("suppressScrollGesture")
        field.isAccessible = true
        field.setBoolean(this, suppress)
    }

    private fun stubVideoSurfaceView(videoWidth: Int, videoHeight: Int) {
        val videoView = mock<View>()
        whenever(videoView.width).thenReturn(videoWidth)
        whenever(videoView.height).thenReturn(videoHeight)
        whenever(mockPlayerView.videoSurfaceView).thenReturn(videoView)
    }

    private fun clickFullscreenButton() {
        val captor = argumentCaptor<View.OnClickListener>()
        verify(mockFullscreen, atLeastOnce()).setOnClickListener(captor.capture())
        captor.lastValue.onClick(mockFullscreen)
    }

    @Test
    fun `test that fullscreen button click zooms to fill and reports fullscreen when at fit`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        clickFullscreenButton()
        assertThat(controller.getZoomLevel()).isEqualTo(2f)
        verify(fullscreenClickedCallback).invoke(true)
    }

    @Test
    fun `test that fullscreen button click resets to fit and reports original when zoomed to fill`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        clickFullscreenButton()
        clickFullscreenButton()
        assertThat(controller.getZoomLevel()).isEqualTo(1f)
        verify(fullscreenClickedCallback).invoke(false)
    }

    @Test
    fun `test that onScale performs haptic feedback when the zoom reaches the fill boundary`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.95f)
        verify(mockPlayerView).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @Test
    fun `test that onScale performs haptic feedback when the zoom returns to the fit boundary`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScale(scaleFactor = 0.68f)
        verify(mockPlayerView, times(1)).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @Test
    fun `test that onScale does not perform haptic feedback when the zoom stays between boundaries`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.5f)
        verify(mockPlayerView, never()).performHapticFeedback(any<Int>())
    }

    @Test
    fun `test that onScale shows the fill screen chip when the zoom reaches the fill boundary`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.95f)
        assertThat(controller.getZoomChipState())
            .isEqualTo(VideoPlayerOverlayChipState.Zoom.FillScreen)
    }

    @Test
    fun `test that onScale shows the percentage chip when the zoom is between boundaries`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getZoomChipState())
            .isEqualTo(VideoPlayerOverlayChipState.Zoom.Percentage(percent = 150))
    }

    @Test
    fun `test that onScale shows the fit to screen chip when the zoom returns to the fit boundary`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScale(scaleFactor = 0.68f)
        assertThat(controller.getZoomChipState())
            .isEqualTo(VideoPlayerOverlayChipState.Zoom.FitToScreen)
    }

    @Test
    fun `test that double tap when zoomed beyond fill returns to fill and shows the fill screen chip`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.setZoomLevel(3f)
        val handled = controller.callOnDoubleTap()
        assertThat(handled).isTrue()
        assertThat(controller.getZoomLevel()).isEqualTo(2f)
        assertThat(controller.getZoomChipState())
            .isEqualTo(VideoPlayerOverlayChipState.Zoom.FillScreen)
        verify(mockPlayerView).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @Test
    fun `test that fullscreen button click shows the fill screen chip when zooming to fill`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        clickFullscreenButton()
        assertThat(controller.getZoomChipState())
            .isEqualTo(VideoPlayerOverlayChipState.Zoom.FillScreen)
    }

    @Test
    fun `test that onScale does not show the zoom chip when gestures are disabled`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController(isGesturesEnabled = false)
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getZoomChipState()).isNull()
    }

    @Test
    fun `test that onScale does not perform haptic feedback when gestures are disabled`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController(isGesturesEnabled = false)
        controller.callOnScale(scaleFactor = 1.95f)
        verify(mockPlayerView, never()).performHapticFeedback(any<Int>())
    }

    @Test
    fun `test that onScale does not show the zoom chip when long press is active`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.runStartLongPressRunnable()
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getZoomChipState()).isNull()
    }

    @Test
    fun `test that startLongPressRunnable clears the zoom chip`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.5f)
        assertThat(controller.getZoomChipState()).isNotNull()
        controller.runStartLongPressRunnable()
        assertThat(controller.getZoomChipState()).isNull()
    }

    @Test
    fun `test that startLongPressRunnable does not call onLongPressActivated when video is not playing`() {
        whenever(mockPlayer.isPlaying).thenReturn(false)
        val controller = createController()
        controller.runStartLongPressRunnable()
        verify(onLongPressActivated, never()).invoke()
        verify(onLongPressSpeedChange, never()).invoke(any())
    }

    @Test
    fun `test that fullscreen button click applies the zoom resize mode without zooming when gestures are disabled`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController(isGesturesEnabled = false)
        clickFullscreenButton()
        verify(fullscreenClickedCallback).invoke(true)
        verify(mockPlayerView).resizeMode = RESIZE_MODE_ZOOM
        assertThat(controller.getZoomLevel()).isEqualTo(1f)
        assertThat(controller.getZoomChipState()).isNull()
    }

    @Test
    fun `test that onScroll pans with the legacy zoom when gestures are disabled`() {
        val controller = createController(isGesturesEnabled = false)
        controller.setLegacyZoomLevel(2f)
        controller.callOnScroll(distanceX = 50f, distanceY = 30f)
        assertThat(controller.getLegacyTranslationX()).isEqualTo(-50f)
        assertThat(controller.getLegacyTranslationY()).isEqualTo(-30f)
        verify(onBrightnessChange, never()).invoke(any())
        verify(onVolumeChange, never()).invoke(any())
    }

    @Test
    fun `test that resetZoom does not zoom to fill when gestures are disabled`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController(isGesturesEnabled = false, isFullscreen = true)
        controller.resetZoom()
        assertThat(controller.getZoomLevel()).isEqualTo(1f)
    }

    @Test
    fun `test that updateGesturesEnabled resets the legacy zoom when the flag turns on`() {
        val controller = createController(isGesturesEnabled = false)
        controller.setLegacyZoomLevel(2f)
        controller.updateGesturesEnabled(true)
        assertThat(controller.getLegacyZoomLevel()).isEqualTo(1f)
        assertThat(controller.getLegacyTranslationX()).isEqualTo(0f)
        assertThat(controller.getLegacyTranslationY()).isEqualTo(0f)
    }

    @Test
    fun `test that video overflow rendering is applied when gestures are enabled`() {
        createController(isGesturesEnabled = true)
        verify(mockPlayerView).clipChildren = false
    }

    @Test
    fun `test that video overflow rendering is not applied when gestures are disabled`() {
        createController(isGesturesEnabled = false)
        verify(mockPlayerView, never()).clipChildren = false
    }

    @Test
    fun `test that updateGesturesEnabled restores video clipping when the flag turns off`() {
        val controller = createController(isGesturesEnabled = true)
        controller.updateGesturesEnabled(false)
        verify(mockPlayerView).clipChildren = true
    }

    @Test
    fun `test that fullscreen button click tracks the fullscreen pressed event when at fit`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        createController()
        clickFullscreenButton()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerFullScreenPressedEvent)
    }

    @Test
    fun `test that fullscreen button click tracks the original pressed event when zoomed to fill`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        createController()
        clickFullscreenButton()
        clickFullscreenButton()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerOriginalPressedEvent)
    }

    @Test
    fun `test that fullscreen button click tracks the fullscreen pressed event when gestures are disabled`() {
        createController(isGesturesEnabled = false)
        clickFullscreenButton()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerFullScreenPressedEvent)
    }

    @Test
    fun `test that onScale does not track the fullscreen pressed event when the pinch crosses the fill level`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScale(scaleFactor = 1.95f)
        verify(fullscreenClickedCallback).invoke(true)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerFullScreenPressedEvent)
    }

    @Test
    fun `test that double tap when zoomed beyond fill does not track the fullscreen pressed event`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.setZoomLevel(3f)
        controller.callOnDoubleTap()
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerFullScreenPressedEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerOriginalPressedEvent)
    }

    @Test
    fun `test that startLongPressRunnable tracks the long press speed event when activated`() {
        val controller = createController()
        controller.runStartLongPressRunnable()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerLongPressSpeedEvent)
    }

    @Test
    fun `test that startLongPressRunnable does not track the long press speed event when current speed is already 2x`() {
        val controller =
            createController(currentSpeedPlayback = VideoSpeedPlaybackItem.PlaybackSpeed_2X)
        controller.runStartLongPressRunnable()
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerLongPressSpeedEvent)
    }

    @Test
    fun `test that onScaleEnd tracks the zoom to fill event when the pinch ends at the fill level`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.95f)
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerZoomToFillEvent)
    }

    @Test
    fun `test that onScaleEnd tracks the zoom to fit event when the pinch ends back at the fit level`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScaleEnd()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 0.68f)
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerZoomToFitEvent)
    }

    @Test
    fun `test that onScaleEnd tracks the pinch to zoom event when the pinch ends between boundaries`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerPinchToZoomEvent)
    }

    @Test
    fun `test that onScaleEnd does not track any zoom event when the zoom level did not change`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScaleBegin()
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker, never()).trackEvent(any())
    }

    @Test
    fun `test that onScaleEnd does not track any zoom event when the pinch only carries float noise`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScaleEnd()
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.000001f)
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker, times(1)).trackEvent(VideoPlayerPinchToZoomEvent)
    }

    @Test
    fun `test that onScaleEnd does not track any zoom event when gestures are disabled`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController(isGesturesEnabled = false)
        controller.callOnScaleBegin()
        controller.callOnScale(scaleFactor = 1.5f)
        controller.callOnScaleEnd()
        verify(mockAnalyticsTracker, never()).trackEvent(any())
    }

    @Test
    fun `test that double tap on the right half tracks the seek forward event`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnDoubleTap(x = 600f)
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerDoubleTapSeekForwardEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekBackwardEvent)
    }

    @Test
    fun `test that double tap on the left half tracks the seek backward event`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.callOnDoubleTap(x = 400f)
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerDoubleTapSeekBackwardEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekForwardEvent)
    }

    @Test
    fun `test that double tap when zoomed beyond fill tracks the zoom to fill event instead of seek events`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        val controller = createController()
        controller.setZoomLevel(3f)
        controller.callOnDoubleTap()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerZoomToFillEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekForwardEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekBackwardEvent)
    }

    @Test
    fun `test that touch up after a brightness swipe tracks the brightness swipe event`() {
        val controller = createController()
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        dispatchTouchUp()
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerBrightnessSwipeEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerVolumeSwipeEvent)
    }

    @Test
    fun `test that touch up after a volume swipe tracks the volume swipe event`() {
        val controller = createController()
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 600f)
        dispatchTouchUp(x = 600f)
        verify(mockAnalyticsTracker).trackEvent(VideoPlayerVolumeSwipeEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerBrightnessSwipeEvent)
    }

    @Test
    fun `test that touch up without a swipe gesture does not track swipe events`() {
        createController()
        dispatchTouchUp()
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerBrightnessSwipeEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerVolumeSwipeEvent)
    }

    @Test
    fun `test that touch cancel after a brightness swipe does not track swipe events`() {
        val controller = createController()
        controller.callOnScroll(distanceX = 0f, distanceY = 50f, startX = 400f)
        dispatchTouchCancel()
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerBrightnessSwipeEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerVolumeSwipeEvent)
    }

    @Test
    fun `test that double tap does not track seek events when the player is null`() {
        stubVideoSurfaceView(videoWidth = 500, videoHeight = 1000)
        whenever(mockPlayerView.player).thenReturn(null)
        val controller = createController()
        controller.callOnDoubleTap(x = 600f)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekForwardEvent)
        verify(mockAnalyticsTracker, never()).trackEvent(VideoPlayerDoubleTapSeekBackwardEvent)
    }
}
