package mega.privacy.android.app.presentation.videoplayer

import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import com.google.common.truth.Truth.assertThat
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    private val playerViewClicked = mock<() -> Unit>()

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
    }

    @After
    fun tearDown() {
        controller?.release()
        controller = null
    }

    private fun createController(
        currentSpeedPlayback: SpeedPlaybackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1X,
        isGesturesEnabled: Boolean = true,
    ) = VideoPlayerController(
        context = activity,
        uiState = VideoPlayerUiState(
            currentSpeedPlayback = currentSpeedPlayback,
            isGesturesEnabled = isGesturesEnabled,
        ),
        container = mockContainer,
        updateRepeatToggleMode = {},
        updateIsVideoOptionPopupShown = {},
        updateIsSpeedOptionsShown = {},
        updateLockStatus = {},
        fullscreenClickedCallback = {},
        lockStateChanged = {},
        playerViewClicked = playerViewClicked,
        onSnapshotSelected = {},
        resetAutoHideTimer = {},
        onLongPressSpeedChange = onLongPressSpeedChange,
        onLongPressActivated = onLongPressActivated,
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

    private fun VideoPlayerController.setZoomLevel(level: Float) {
        val field = VideoPlayerController::class.java.getDeclaredField("zoomLevel")
        field.isAccessible = true
        field.setFloat(this, level)
    }

    private fun VideoPlayerController.getTranslationX(): Float {
        val field = VideoPlayerController::class.java.getDeclaredField("translationX")
        field.isAccessible = true
        return field.getFloat(this)
    }

    private fun VideoPlayerController.getTranslationY(): Float {
        val field = VideoPlayerController::class.java.getDeclaredField("translationY")
        field.isAccessible = true
        return field.getFloat(this)
    }

    private fun VideoPlayerController.callOnScroll(distanceX: Float = 50f, distanceY: Float = 50f) {
        val gdField = VideoPlayerController::class.java.getDeclaredField("gestureDetector")
        gdField.isAccessible = true
        val gd = gdField.get(this) as? GestureDetector ?: return
        val listenerField = GestureDetector::class.java.getDeclaredField("mListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(gd) as? GestureDetector.OnGestureListener ?: return
        val e2 = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 100f, 100f, 0)
        try {
            listener.onScroll(null, e2, distanceX, distanceY)
        } finally {
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
    fun `test that onSingleTapConfirmed calls playerViewClicked`() {
        val controller = createController()
        controller.callOnSingleTapConfirmed()
        verify(playerViewClicked).invoke()
    }
}
