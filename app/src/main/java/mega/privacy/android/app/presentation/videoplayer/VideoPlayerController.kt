package mega.privacy.android.app.presentation.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerOverlayChip
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerOverlayChipState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.mobile.analytics.event.VideoPlayerRotateToLandscapePressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerRotateToPortraitPressedEvent
import timber.log.Timber
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
class VideoPlayerController(
    private val context: Context,
    private val uiState: VideoPlayerUiState,
    container: ViewGroup,
    private val updateRepeatToggleMode: () -> Unit,
    private val updateIsVideoOptionPopupShown: (Boolean) -> Unit,
    private val updateIsSpeedOptionsShown: (Boolean) -> Unit,
    private val updateLockStatus: (Boolean) -> Unit,
    private val fullscreenClickedCallback: (Boolean) -> Unit,
    private val lockStateChanged: (lock: Boolean) -> Unit,
    private val playerViewClicked: () -> Unit,
    private val onSnapshotSelected: () -> Unit,
    private val resetAutoHideTimer: () -> Unit,
    private val onLongPressSpeedChange: (SpeedPlaybackItem) -> Unit,
    private val onLongPressActivated: () -> Unit,
) {
    private val repeatToggleButton = container.findViewById<ImageButton>(R.id.repeat_toggle)
    private val playerComposeView = container.findViewById<PlayerView>(R.id.player_compose_view)
    private val moreOptionButton = container.findViewById<ImageButton>(R.id.more_option)
    private val fullscreenButton = container.findViewById<ImageButton>(R.id.full_screen)
    private val controllerView = container.findViewById<View>(R.id.layout_player)
    private val unlockView = container.findViewById<View>(R.id.layout_unlock)
    private val unlockButton = container.findViewById<ImageButton>(R.id.image_button_unlock)
    private val speedPlaybackButton = container.findViewById<TextView>(R.id.speed_playback)
    private val deviceRotateButton = container.findViewById<ImageButton>(R.id.device_rotated)

    private val rewButton = container.findViewById<ImageButton>(R.id.exo_rew)
    private val ffwdButton = container.findViewById<ImageButton>(R.id.exo_ffwd)

    // Overlay chip views — created programmatically so they remain visible even when
    // the player controller is auto-hidden.
    private lateinit var seekChipView: ComposeView
    private lateinit var longPressChipView: ComposeView
    private val seekChipState = mutableStateOf<VideoPlayerOverlayChipState?>(null)
    private val longPressChipState = mutableStateOf<VideoPlayerOverlayChipState?>(null)

    private val seekHandler = Handler(Looper.getMainLooper())
    private var accumulatedSeekMs = 0L
    private val hideSeekChipRunnable = Runnable {
        accumulatedSeekMs = 0L
        seekChipState.value = null
    }

    private var currentSpeedPlayback: SpeedPlaybackItem = uiState.currentSpeedPlayback
    private var longPressSavedSpeed: SpeedPlaybackItem? = null
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var isLongPressActive = false

    private val startLongPressRunnable = Runnable {
        val speed = currentSpeedPlayback
        if (speed == VideoSpeedPlaybackItem.PlaybackSpeed_2X) return@Runnable
        longPressSavedSpeed = speed
        isLongPressActive = true
        playerComposeView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onLongPressActivated()
        longPressChipState.value = VideoPlayerOverlayChipState.LongPressSpeedHeld(
            speedText = VideoSpeedPlaybackItem.PlaybackSpeed_2X.text,
        )
        onLongPressSpeedChange(VideoSpeedPlaybackItem.PlaybackSpeed_2X)
    }

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var zoomLevel = 1.0f
    private val maxZoom = 5.0f
    private var translationX = 0f
    private var translationY = 0f

    private var isFullscreen = mutableStateOf(uiState.isFullscreen)
    private var playbackState = uiState.mediaPlaybackState
    private var isLocked = mutableStateOf(uiState.isLocked)
    private var isGesturesEnabled = uiState.isGesturesEnabled
    private var playQueueInOverflowMenu = mutableStateOf(uiState.items.size > SINGLE_PLAYLIST_SIZE)

    init {
        initChipOverlays()
        playerComposeView.setControllerAnimationEnabled(false)
        setupRepeatToggleButton(uiState.repeatToggleMode)
        setupMoreOptionButton()
        updatePlayQueueOverflowMenuItems(uiState.items.size)
        setupFullscreen(uiState.isFullscreen)
        setupLockButton()
        setupSpeedPlaybackButton()
        setupSeekButtons()
        setupGestures()
        setupDeviceRotateButton()
    }

    private fun initChipOverlays() {
        fun Int.toPx(): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()

        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        seekChipView = ComposeView(context)
        seekChipView.setupComposeView(context) { VideoPlayerOverlayChip(seekChipState.value) }
        val seekBottomMarginDp =
            if (isLandscape) SEEK_CHIP_BOTTOM_MARGIN_LAND_DP else SEEK_CHIP_BOTTOM_MARGIN_PORT_DP
        playerComposeView.addView(
            seekChipView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).also { it.bottomMargin = seekBottomMarginDp.toPx() },
        )

        longPressChipView = ComposeView(context)
        longPressChipView.setupComposeView(context) {
            VideoPlayerOverlayChip(longPressChipState.value)
        }
        val longPressChipBottomMarginDp = if (isLandscape) {
            SEEK_CHIP_BOTTOM_MARGIN_LAND_DP
        } else {
            SEEK_CHIP_BOTTOM_MARGIN_PORT_DP + LONG_PRESS_CHIP_PORTRAIT_EXTRA_DP
        }
        playerComposeView.addView(
            longPressChipView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).also { it.bottomMargin = longPressChipBottomMarginDp.toPx() },
        )
    }

    /**
     * Set up the repeat toggle button
     *
     * @param defaultRepeatToggleMode the default RepeatToggleMode
     */
    private fun setupRepeatToggleButton(defaultRepeatToggleMode: RepeatToggleMode) {
        repeatToggleButton.isVisible = true
        updateRepeatToggleButtonUI(context, defaultRepeatToggleMode)
        repeatToggleButton.setOnClickListener {
            updateRepeatToggleMode()
            resetAutoHideTimer()
        }
    }

    /**
     * Update repeat toggle button UI
     *
     * @param context Context
     * @param repeatToggleMode the current RepeatToggleMode
     */
    internal fun updateRepeatToggleButtonUI(
        context: Context,
        repeatToggleMode: RepeatToggleMode,
    ) {
        repeatToggleButton.setColorFilter(
            if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                context.getColor(R.color.white)
            } else {
                context.getColor(R.color.color_button_brand)
            }
        )
    }

    /**
     * Updates whether VideoPlayerMoreOption.Playlist appears in the overflow menu (same rule as legacy toolbar playlist).
     */
    internal fun updatePlayQueueOverflowMenuItems(itemSize: Int) {
        playQueueInOverflowMenu.value = itemSize > SINGLE_PLAYLIST_SIZE
    }

    private fun setupMoreOptionButton() {
        moreOptionButton.setOnClickListener {
            updateIsVideoOptionPopupShown(true)
        }
    }

    internal fun onSnapshotOptionSelected() {
        onSnapshotSelected()
    }

    internal fun onLockOptionSelected() {
        updateLockState(true)
    }

    private fun ComposeView.setupComposeView(context: Context, content: @Composable () -> Unit) {
        (context as? AppCompatActivity)?.let { activity ->
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            content()
        }
    }

    internal fun setupFullscreen(isFullScreen: Boolean) {
        updateFullscreenButtonIcon(isFullScreen)
        fullscreenButton.setOnClickListener {
            isFullscreen.value = !isFullscreen.value
            fullscreenClickedCallback(isFullscreen.value)
            resetAutoHideTimer()
        }
    }

    internal fun updateFullscreenButtonIcon(isFullScreen: Boolean) {
        isFullscreen.value = isFullScreen
        fullscreenButton.setImageResource(
            if (isFullScreen) {
                R.drawable.ic_original
            } else {
                R.drawable.ic_full_screen
            }
        )
    }

    private fun setupDeviceRotateButton() {
        deviceRotateButton.setOnClickListener {
            val activity = context as? Activity
            if (activity == null) {
                Timber.e("Context is not an activity")
                return@setOnClickListener
            }
            val isPortrait =
                context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            activity.requestedOrientation = if (isPortrait) {
                Analytics.tracker.trackEvent(VideoPlayerRotateToLandscapePressedEvent)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                Analytics.tracker.trackEvent(VideoPlayerRotateToPortraitPressedEvent)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            resetAutoHideTimer()
        }
    }

    private fun setupLockButton() {
        updateLockState(uiState.isLocked)
        unlockButton.setOnClickListener {
            updateLockState(false)
        }
    }

    private fun updateLockState(isLock: Boolean) {
        isLocked.value = isLock
        controllerView.isVisible = !isLock
        unlockView.isVisible = isLock
        lockStateChanged(isLock)
        updateLockStatus(isLock)
    }

    internal fun updateLockView(isLock: Boolean) {
        isLocked.value = isLock
        controllerView.isVisible = !isLock
        unlockView.isVisible = isLock
    }

    internal fun updateSpeedPlaybackButtonIcon(text: String) {
        speedPlaybackButton.text = text
    }

    private fun setupSpeedPlaybackButton() {
        speedPlaybackButton.text = uiState.currentSpeedPlayback.text
        speedPlaybackButton.setOnClickListener {
            updateIsSpeedOptionsShown(true)
        }
    }

    internal fun updatePlaybackState(state: MediaPlaybackState) {
        playbackState = state
    }

    internal fun updateGesturesEnabled(enabled: Boolean) {
        isGesturesEnabled = enabled
    }

    internal fun updateCurrentSpeedPlayback(item: SpeedPlaybackItem) {
        currentSpeedPlayback = item
    }

    private fun setupSeekButtons() {
        rewButton?.setOnClickListener {
            if (!isLocked.value) {
                seekByDelta(-SEEK_STEP.inWholeMilliseconds)
                resetAutoHideTimer()
            }
        }
        ffwdButton?.setOnClickListener {
            if (!isLocked.value) {
                seekByDelta(+SEEK_STEP.inWholeMilliseconds)
                resetAutoHideTimer()
            }
        }
    }

    private fun seekByDelta(deltaMs: Long) {
        val player = playerComposeView.player ?: return
        val newPosition = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(newPosition)

        if (!isGesturesEnabled) return

        if (accumulatedSeekMs != 0L && (accumulatedSeekMs > 0) != (deltaMs > 0)) {
            accumulatedSeekMs = deltaMs
        } else {
            accumulatedSeekMs += deltaMs
        }

        val absoluteSeconds = (abs(accumulatedSeekMs) / 1000L).toInt()
        seekChipState.value = VideoPlayerOverlayChipState.Seek(
            seconds = absoluteSeconds,
            isForward = accumulatedSeekMs > 0,
        )

        seekHandler.removeCallbacks(hideSeekChipRunnable)
        seekHandler.postDelayed(hideSeekChipRunnable, SEEK_CHIP_HIDE_DELAY.inWholeMilliseconds)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (!isLocked.value) {
                        zoomLevel = (zoomLevel * detector.scaleFactor).coerceIn(1.0f, maxZoom)
                        updateTransformations()
                    }
                    return true
                }
            })

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    if (isLongPressActive) return true
                    if (zoomLevel > 1 && !isLocked.value) {
                        translationX -= distanceX
                        translationY -= distanceY
                        enforceBoundaries()
                        updateTransformations()
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    playerViewClicked()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isLocked.value || !isGesturesEnabled) return false
                    val viewWidth = playerComposeView.width
                    if (viewWidth == 0) return false
                    if (e.x < viewWidth / 2f) {
                        seekByDelta(-SEEK_STEP.inWholeMilliseconds)
                    } else {
                        seekByDelta(+SEEK_STEP.inWholeMilliseconds)
                    }
                    resetAutoHideTimer()
                    return true
                }
            })

        playerComposeView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isGesturesEnabled && !isLocked.value) {
                        longPressHandler.postDelayed(
                            startLongPressRunnable,
                            LONG_PRESS_TIMEOUT_MS,
                        )
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    longPressHandler.removeCallbacks(startLongPressRunnable)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(startLongPressRunnable)
                    if (isLongPressActive) {
                        releaseLongPress()
                    }
                }
            }
            scaleGestureDetector?.onTouchEvent(event)
            gestureDetector?.onTouchEvent(event)
            true
        }
    }

    private fun releaseLongPress() {
        isLongPressActive = false
        longPressChipState.value = null
        longPressSavedSpeed?.let { onLongPressSpeedChange(it) }
        longPressSavedSpeed = null
    }

    private fun updateTransformations() {
        (playerComposeView.videoSurfaceView as? TextureView)?.let { textureView ->
            val matrix = Matrix()
            matrix.postScale(zoomLevel, zoomLevel, textureView.width / 2f, textureView.height / 2f)
            matrix.postTranslate(translationX, translationY)
            textureView.setTransform(matrix)
            if (playbackState == MediaPlaybackState.Paused) {
                textureView.invalidate()
                textureView.requestLayout()
            }
        }
    }

    private fun enforceBoundaries() {
        playerComposeView.videoSurfaceView?.let { textureView ->
            val maxTranslationX = (zoomLevel - 1) * textureView.width / 2
            val maxTranslationY = (zoomLevel - 1) * textureView.height / 2

            translationX = translationX.coerceIn(-maxTranslationX, maxTranslationX)
            translationY = translationY.coerceIn(-maxTranslationY, maxTranslationY)
        }
    }

    internal fun release() {
        repeatToggleButton?.setOnClickListener(null)
        moreOptionButton?.setOnClickListener(null)
        fullscreenButton?.setOnClickListener(null)
        unlockButton?.setOnClickListener(null)
        speedPlaybackButton?.setOnClickListener(null)
        deviceRotateButton?.setOnClickListener(null)
        rewButton?.setOnClickListener(null)
        ffwdButton?.setOnClickListener(null)

        seekHandler.removeCallbacks(hideSeekChipRunnable)
        seekChipState.value = null
        playerComposeView.removeView(seekChipView)

        longPressHandler.removeCallbacks(startLongPressRunnable)
        if (isLongPressActive) releaseLongPress()
        playerComposeView.removeView(longPressChipView)

        playerComposeView?.setOnTouchListener(null)
        scaleGestureDetector = null
        gestureDetector = null
    }

    companion object {
        private val SEEK_STEP = 15.seconds
        private val SEEK_CHIP_HIDE_DELAY = 3.seconds

        // Portrait: positions seek chip above the controller bar (~128 dp tall, 30 dp margin).
        private const val SEEK_CHIP_BOTTOM_MARGIN_PORT_DP = 145

        // Landscape: centers seek chip between the timebar and the play/pause button.
        private const val SEEK_CHIP_BOTTOM_MARGIN_LAND_DP = 110

        // Positions long-press chip 30dp above the seek chip in portrait (per design spec).
        private const val LONG_PRESS_CHIP_PORTRAIT_EXTRA_DP = 30
        // Fires 300ms after touch-down — faster than the system default (~500ms) for a snappier feel.
        private const val LONG_PRESS_TIMEOUT_MS = 300L
    }
}
