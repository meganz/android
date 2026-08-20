package mega.privacy.android.app.presentation.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.feature.mediaplayer.components.GestureSliderType
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerGestureSlider
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerOverlayChip
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerOverlayChipState
import mega.privacy.mobile.analytics.event.VideoPlayerRotateToLandscapePressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerRotateToPortraitPressedEvent
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
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
    private val onBrightnessChange: (Float) -> Unit,
    private val onVolumeChange: (Float) -> Unit,
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
    private lateinit var zoomChipView: ComposeView
    private val seekChipState = mutableStateOf<VideoPlayerOverlayChipState?>(null)
    private val longPressChipState = mutableStateOf<VideoPlayerOverlayChipState?>(null)
    private val zoomChipState = mutableStateOf<VideoPlayerOverlayChipState?>(null)

    private val zoomChipHandler = Handler(Looper.getMainLooper())
    private val hideZoomChipRunnable = Runnable { zoomChipState.value = null }

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
        // Playback may have been paused between ACTION_DOWN and the long-press timeout.
        if (playerComposeView.player?.isPlaying != true) return@Runnable
        val speed = currentSpeedPlayback
        if (speed == VideoSpeedPlaybackItem.PlaybackSpeed_2X) return@Runnable
        longPressSavedSpeed = speed
        isLongPressActive = true
        playerComposeView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onLongPressActivated()
        // Only one overlay chip at a time: drop lingering seek/zoom chips immediately
        // instead of letting them outlive their hide delay next to the 2x chip.
        seekHandler.removeCallbacks(hideSeekChipRunnable)
        hideSeekChipRunnable.run()
        zoomChipHandler.removeCallbacks(hideZoomChipRunnable)
        hideZoomChipRunnable.run()
        longPressChipState.value = VideoPlayerOverlayChipState.LongPressSpeedHeld(
            speedText = VideoSpeedPlaybackItem.PlaybackSpeed_2X.text,
        )
        onLongPressSpeedChange(VideoSpeedPlaybackItem.PlaybackSpeed_2X)
    }

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private val zoomState = VideoPlayerZoomState()

    // Pinch state for the pre-gestures behaviour, kept while the VideoPlayerGestures feature
    // flag is off: a plain 1x-5x TextureView-matrix zoom inside the FIT frame, with fullscreen
    // driven by the resize mode instead of zooming. Everything the flag adds (snapping,
    // haptics, the zoom chip, fill-screen zooming) must stay invisible until it is on.
    private var legacyZoomLevel = 1f
    private var legacyTranslationX = 0f
    private var legacyTranslationY = 0f

    private var isFullscreen = mutableStateOf(uiState.isFullscreen)
    private var isLocked = mutableStateOf(uiState.isLocked)
    private var isGesturesEnabled = uiState.isGesturesEnabled
    private var playQueueInOverflowMenu = mutableStateOf(uiState.items.size > SINGLE_PLAYLIST_SIZE)

    private enum class ScrollGestureType { None, Pan, Brightness, Volume }

    private var scrollGestureType = ScrollGestureType.None

    // Suppresses brightness/volume gestures after a multi-touch (pinch) sequence until the
    // user starts a completely fresh touch (ACTION_DOWN). Prevents the finger remaining on
    // screen after a pinch from accidentally triggering brightness/volume adjustment.
    private var suppressScrollGesture = false
    private var gestureStartBrightness = 0f
    private var gestureStartVolume = 0f
    private val brightnessSliderState = mutableStateOf<Float?>(null)
    private val volumeSliderState = mutableStateOf<Float?>(null)
    private var brightnessSliderView: ComposeView? = null
    private var volumeSliderView: ComposeView? = null
    private val sliderHideHandler = Handler(Looper.getMainLooper())
    private val hideBrightnessSliderRunnable = Runnable { brightnessSliderState.value = null }
    private val hideVolumeSliderRunnable = Runnable { volumeSliderState.value = null }

    // Re-applies the fill-screen display when the FIT rect changes (controller recreation
    // on rotation, PiP resize, new media item) so the fullscreen choice survives relayout.
    private val videoLayoutChangeListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val sizeChanged =
                right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop
            if (sizeChanged && isFullscreen.value && isGesturesEnabled) {
                zoomState.zoomToFill(currentViewport())
                updateTransformations()
            }
        }

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
        if (isGesturesEnabled) setupVideoOverflowRendering()
        playerComposeView.videoSurfaceView?.addOnLayoutChangeListener(videoLayoutChangeListener)
        if (!isGesturesEnabled) applyLegacyResizeMode(isFullscreen.value)
    }

    /**
     * Allows the zoomed video to draw beyond the FIT (letterboxed) content frame so it can
     * gradually fill, and then overflow, the screen. The video is still cropped at the
     * [PlayerView] bounds, i.e. the screen edges. Gestures-flag-on only: the parent clipping
     * is what keeps the legacy matrix zoom inside the letterboxed frame.
     */
    private fun setupVideoOverflowRendering() {
        (playerComposeView.videoSurfaceView?.parent as? ViewGroup)?.clipChildren = false
        playerComposeView.clipChildren = false
    }

    private fun restoreVideoClipping() {
        (playerComposeView.videoSurfaceView?.parent as? ViewGroup)?.clipChildren = true
        playerComposeView.clipChildren = true
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

        zoomChipView = ComposeView(context)
        zoomChipView.setupComposeView(context) { VideoPlayerOverlayChip(zoomChipState.value) }
        playerComposeView.addView(
            zoomChipView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).also { it.bottomMargin = seekBottomMarginDp.toPx() },
        )

        val sliderSideMarginDp = if (isLandscape) {
            SLIDER_SIDE_MARGIN_LAND_DP
        } else {
            SLIDER_SIDE_MARGIN_PORT_DP
        }

        val brightnessComposeView = ComposeView(context)
        brightnessComposeView.setupComposeView(context) {
            val value = brightnessSliderState.value
            if (value != null) {
                VideoPlayerGestureSlider(value = value, type = GestureSliderType.Brightness)
            }
        }
        playerComposeView.addView(
            brightnessComposeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL or Gravity.START,
            ).also { it.marginStart = sliderSideMarginDp.toPx() },
        )
        brightnessSliderView = brightnessComposeView

        val volumeComposeView = ComposeView(context)
        volumeComposeView.setupComposeView(context) {
            val value = volumeSliderState.value
            if (value != null) {
                VideoPlayerGestureSlider(value = value, type = GestureSliderType.Volume)
            }
        }
        playerComposeView.addView(
            volumeComposeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL or Gravity.END,
            ).also { it.marginEnd = sliderSideMarginDp.toPx() },
        )
        volumeSliderView = volumeComposeView
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
            if (isGesturesEnabled) {
                val viewport = currentViewport()
                if (zoomState.isZoomedToFill(viewport)) {
                    zoomState.reset()
                } else {
                    zoomState.zoomToFill(viewport)
                }
                updateTransformations()
                showZoomChipWithAutoHide(viewport)
                syncFullscreenState()
            } else {
                isFullscreen.value = !isFullscreen.value
                applyLegacyResizeMode(isFullscreen.value)
                fullscreenClickedCallback(isFullscreen.value)
            }
            resetAutoHideTimer()
        }
    }

    // With the gestures flag off, fullscreen is still the resize-mode crop it was before
    // the flag existed.
    private fun applyLegacyResizeMode(isFullScreen: Boolean) {
        playerComposeView.resizeMode = if (isFullScreen) RESIZE_MODE_ZOOM else RESIZE_MODE_FIT
    }

    // Fullscreen now means "zoomed to fill": any change that moves the zoom across the
    // fill level must keep the button icon and the ViewModel state in agreement with the
    // actual display, otherwise the button acts on stale state.
    private fun syncFullscreenState() {
        val zoomedToFill = zoomState.isZoomedToFill(currentViewport())
        if (isFullscreen.value != zoomedToFill) {
            updateFullscreenButtonIcon(zoomedToFill)
            fullscreenClickedCallback(zoomedToFill)
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

    internal fun updateGesturesEnabled(enabled: Boolean) {
        if (isGesturesEnabled == enabled) return
        isGesturesEnabled = enabled
        // The flag arrives async after construction; clear the other path's zoom so a
        // pinch made before the switch cannot leave a stale transform behind.
        if (enabled) {
            legacyZoomLevel = 1f
            legacyTranslationX = 0f
            legacyTranslationY = 0f
            (playerComposeView.videoSurfaceView as? TextureView)?.setTransform(Matrix())
            playerComposeView.resizeMode = RESIZE_MODE_FIT
            setupVideoOverflowRendering()
            resetZoom()
        } else {
            zoomState.reset()
            updateTransformations()
            restoreVideoClipping()
            applyLegacyResizeMode(isFullscreen.value)
        }
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

        // The zoom chip shares the seek chip's position — drop it before showing the seek chip.
        zoomChipHandler.removeCallbacks(hideZoomChipRunnable)
        hideZoomChipRunnable.run()

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
                    if (isLocked.value) return true
                    if (!isGesturesEnabled) {
                        legacyZoomLevel = (legacyZoomLevel * detector.scaleFactor)
                            .coerceIn(VideoPlayerZoomState.MIN_ZOOM, VideoPlayerZoomState.MAX_ZOOM)
                        updateLegacyTransformations()
                        return true
                    }
                    val viewport = currentViewport()
                    val reachedBoundary = zoomState.onPinchScale(detector.scaleFactor, viewport)
                    // Overlay feedback is suppressed while the 2x chip is held — only one
                    // overlay chip at a time.
                    val showZoomFeedback = !isLongPressActive
                    if (reachedBoundary != null && showZoomFeedback) {
                        performBoundaryHapticFeedback()
                    }
                    updateTransformations()
                    if (showZoomFeedback) showZoomChip(viewport)
                    syncFullscreenState()
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    zoomState.onPinchEnd()
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
                    if (!isGesturesEnabled) {
                        if (legacyZoomLevel > 1f && !isLocked.value) {
                            legacyTranslationX -= distanceX
                            legacyTranslationY -= distanceY
                            enforceLegacyBoundaries()
                            updateLegacyTransformations()
                        }
                        return true
                    }
                    val viewport = currentViewport()
                    if (scrollGestureType == ScrollGestureType.None && !isLocked.value &&
                        canStartPan(viewport, distanceX, distanceY)
                    ) {
                        scrollGestureType = ScrollGestureType.Pan
                        // A pan drag must not turn into the long-press speed gesture.
                        longPressHandler.removeCallbacks(startLongPressRunnable)
                    }
                    if (scrollGestureType == ScrollGestureType.Pan) {
                        if (!isLocked.value) {
                            zoomState.onPan(-distanceX, -distanceY, viewport)
                            updateTransformations()
                        }
                        return true
                    }
                    if (!isLocked.value && e1 != null && e2.pointerCount == 1 && !suppressScrollGesture) {
                        if (scrollGestureType == ScrollGestureType.None) {
                            if (abs(distanceY) > abs(distanceX)) {
                                if (isLeftHalfOfScreen(e1.x)) {
                                    scrollGestureType = ScrollGestureType.Brightness
                                    gestureStartBrightness = readCurrentBrightness()
                                } else {
                                    scrollGestureType = ScrollGestureType.Volume
                                    gestureStartVolume = readCurrentVolume()
                                }
                                // Cancel long-press so the speed overlay cannot activate
                                // during a brightness/volume swipe, and so that onScroll
                                // is never blocked by isLongPressActive = true mid-swipe.
                                longPressHandler.removeCallbacks(startLongPressRunnable)
                                // Hide instantly (not with the post-gesture delay) so both sliders never show at once.
                                if (scrollGestureType == ScrollGestureType.Brightness) {
                                    sliderHideHandler.removeCallbacks(hideVolumeSliderRunnable)
                                    volumeSliderState.value = null
                                } else {
                                    sliderHideHandler.removeCallbacks(hideBrightnessSliderRunnable)
                                    brightnessSliderState.value = null
                                }
                            }
                        }
                        when (scrollGestureType) {
                            ScrollGestureType.Brightness -> handleBrightnessScroll(distanceY)
                            ScrollGestureType.Volume -> handleVolumeScroll(distanceY)
                            else -> {}
                        }
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    playerViewClicked()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isLocked.value || !isGesturesEnabled) return false
                    val viewport = currentViewport()
                    if (zoomState.isZoomedBeyondFill(viewport)) {
                        zoomState.zoomToFill(viewport)
                        performBoundaryHapticFeedback()
                        updateTransformations()
                        // The trailing ACTION_UP of the double tap schedules the chip hide.
                        showZoomChip(viewport)
                        syncFullscreenState()
                        return true
                    }
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
                    suppressScrollGesture = false
                    // The 2x speed gesture is only available while the video is playing.
                    if (isGesturesEnabled && !isLocked.value &&
                        playerComposeView.player?.isPlaying == true
                    ) {
                        longPressHandler.postDelayed(
                            startLongPressRunnable,
                            LONG_PRESS_TIMEOUT.inWholeMilliseconds,
                        )
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    suppressScrollGesture = true
                    longPressHandler.removeCallbacks(startLongPressRunnable)
                    scrollGestureType = ScrollGestureType.None
                    sliderHideHandler.removeCallbacks(hideBrightnessSliderRunnable)
                    sliderHideHandler.removeCallbacks(hideVolumeSliderRunnable)
                    brightnessSliderState.value = null
                    volumeSliderState.value = null
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(startLongPressRunnable)
                    if (isLongPressActive) {
                        releaseLongPress()
                    }
                    scrollGestureType = ScrollGestureType.None
                    if (brightnessSliderState.value != null) {
                        sliderHideHandler.removeCallbacks(hideBrightnessSliderRunnable)
                        sliderHideHandler.postDelayed(
                            hideBrightnessSliderRunnable,
                            SLIDER_HIDE_DELAY.inWholeMilliseconds
                        )
                    }
                    if (volumeSliderState.value != null) {
                        sliderHideHandler.removeCallbacks(hideVolumeSliderRunnable)
                        sliderHideHandler.postDelayed(
                            hideVolumeSliderRunnable,
                            SLIDER_HIDE_DELAY.inWholeMilliseconds
                        )
                    }
                    if (zoomChipState.value != null) {
                        zoomChipHandler.removeCallbacks(hideZoomChipRunnable)
                        zoomChipHandler.postDelayed(
                            hideZoomChipRunnable,
                            ZOOM_CHIP_HIDE_DELAY.inWholeMilliseconds
                        )
                    }
                }
            }
            scaleGestureDetector?.onTouchEvent(event)
            gestureDetector?.onTouchEvent(event)
            true
        }
    }

    // Beyond fill both axes pan. Between fit and fill only one axis overflows, so the
    // pan claims the gesture only when the swipe direction matches that axis, leaving
    // the other direction to the brightness/volume gestures.
    private fun canStartPan(
        viewport: VideoZoomViewport,
        distanceX: Float,
        distanceY: Float,
    ): Boolean = when {
        zoomState.isZoomedBeyondFill(viewport) -> true
        abs(distanceX) > abs(distanceY) -> zoomState.canPanHorizontally(viewport)
        else -> zoomState.canPanVertically(viewport)
    }

    // CONFIRM maps to the system's click-strength waveform (the same effect YouTube-style
    // players use) and stays gated by the system touch-feedback setting; CLOCK_TICK is
    // imperceptible on some budget devices (e.g. Samsung A23). VIRTUAL_KEY is the closest
    // strong effect below API 30.
    private fun performBoundaryHapticFeedback() {
        playerComposeView.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
        )
    }

    private fun showZoomChip(viewport: VideoZoomViewport) {
        // Only one overlay chip at a time: the zoom chip shares the seek chip's position,
        // so a lingering seek chip is dropped immediately.
        seekHandler.removeCallbacks(hideSeekChipRunnable)
        hideSeekChipRunnable.run()
        zoomChipHandler.removeCallbacks(hideZoomChipRunnable)
        zoomChipState.value = when (zoomState.currentBoundary(viewport)) {
            ZoomBoundary.Fit -> VideoPlayerOverlayChipState.Zoom.FitToScreen
            ZoomBoundary.Fill -> VideoPlayerOverlayChipState.Zoom.FillScreen
            null -> VideoPlayerOverlayChipState.Zoom.Percentage(
                percent = (zoomState.zoomLevel * 100).roundToInt(),
            )
        }
    }

    // For chip triggers outside the player touch listener (e.g. the fullscreen button),
    // where no ACTION_UP will schedule the hide.
    private fun showZoomChipWithAutoHide(viewport: VideoZoomViewport) {
        showZoomChip(viewport)
        zoomChipHandler.postDelayed(hideZoomChipRunnable, ZOOM_CHIP_HIDE_DELAY.inWholeMilliseconds)
    }

    private fun releaseLongPress() {
        isLongPressActive = false
        longPressChipState.value = null
        longPressSavedSpeed?.let { onLongPressSpeedChange(it) }
        longPressSavedSpeed = null
    }

    /**
     * Clears any gesture zoom/pan and restores the current display mode — fit, or the
     * fill-screen level when fullscreen is enabled. No-op while the gestures flag is off,
     * where fullscreen is a resize mode and the pinch zoom is never reset (pre-flag behaviour).
     */
    internal fun resetZoom() {
        if (!isGesturesEnabled) return
        if (isFullscreen.value) {
            zoomState.zoomToFill(currentViewport())
        } else {
            zoomState.reset()
        }
        updateTransformations()
    }

    private fun handleBrightnessScroll(distanceY: Float) {
        val playerHeight = playerComposeView.height.takeIf { it > 0 } ?: return
        val delta = distanceY / playerHeight * GESTURE_SCROLL_SENSITIVITY
        val newValue =
            ((brightnessSliderState.value ?: gestureStartBrightness) + delta).coerceIn(0f, 1f)
        brightnessSliderState.value = newValue
        onBrightnessChange(newValue)
    }

    private fun handleVolumeScroll(distanceY: Float) {
        val playerHeight = playerComposeView.height.takeIf { it > 0 } ?: return
        val delta = distanceY / playerHeight * GESTURE_SCROLL_SENSITIVITY
        val newValue = ((volumeSliderState.value ?: gestureStartVolume) + delta).coerceIn(0f, 1f)
        volumeSliderState.value = newValue
        onVolumeChange(newValue)
    }

    private fun readCurrentBrightness(): Float {
        // Android reports -1f when the window uses system default brightness; read the system
        // setting so the slider starts at the actual display level rather than jumping to 50%.
        val windowBrightness = (context as? Activity)?.window?.attributes?.screenBrightness ?: -1f
        if (windowBrightness >= 0f) return windowBrightness
        return runCatching {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
            )
            (raw.toFloat() / SYSTEM_BRIGHTNESS_MAX).coerceIn(0f, 1f)
        }.getOrElse {
            Timber.e(it, "Failed to read system screen brightness")
            DEFAULT_BRIGHTNESS_FALLBACK
        }
    }

    private fun readCurrentVolume(): Float {
        return runCatching {
            val am = context.getSystemService(AudioManager::class.java)
                ?: return DEFAULT_VOLUME_FALLBACK
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max == 0) 0f else am.getStreamVolume(AudioManager.STREAM_MUSIC)
                .toFloat() / max.toFloat()
        }.getOrElse {
            Timber.e(it, "Failed to read current volume")
            DEFAULT_VOLUME_FALLBACK
        }
    }

    // Scales the video view itself (pivot = view center = screen center, since the FIT
    // content frame is centered in the PlayerView) instead of applying a TextureView
    // matrix, so the zoomed frame can grow beyond the letterboxed FIT rect and is only
    // cropped at the screen edges. This also keeps media3's own rotation transform intact.
    private fun updateTransformations() {
        playerComposeView.videoSurfaceView?.let { videoView ->
            videoView.scaleX = zoomState.zoomLevel
            videoView.scaleY = zoomState.zoomLevel
            videoView.translationX = zoomState.translationX
            videoView.translationY = zoomState.translationY
        }
    }

    private fun updateLegacyTransformations() {
        (playerComposeView.videoSurfaceView as? TextureView)?.let { textureView ->
            val matrix = Matrix()
            matrix.postScale(
                legacyZoomLevel,
                legacyZoomLevel,
                textureView.width / 2f,
                textureView.height / 2f,
            )
            matrix.postTranslate(legacyTranslationX, legacyTranslationY)
            textureView.setTransform(matrix)
            // A non-playing TextureView does not redraw on its own after a transform change.
            if (playerComposeView.player?.isPlaying != true) {
                textureView.invalidate()
                textureView.requestLayout()
            }
        }
    }

    private fun enforceLegacyBoundaries() {
        playerComposeView.videoSurfaceView?.let { videoView ->
            val maxTranslationX = (legacyZoomLevel - 1) * videoView.width / 2
            val maxTranslationY = (legacyZoomLevel - 1) * videoView.height / 2

            legacyTranslationX = legacyTranslationX.coerceIn(-maxTranslationX, maxTranslationX)
            legacyTranslationY = legacyTranslationY.coerceIn(-maxTranslationY, maxTranslationY)
        }
    }

    private fun currentViewport(): VideoZoomViewport {
        val videoView = playerComposeView.videoSurfaceView
        return VideoZoomViewport(
            videoWidth = videoView?.width ?: 0,
            videoHeight = videoView?.height ?: 0,
            screenWidth = playerComposeView.width,
            screenHeight = playerComposeView.height,
        )
    }

    private fun isLeftHalfOfScreen(x: Float): Boolean = x < playerComposeView.width / 2f

    internal fun release() {
        repeatToggleButton?.setOnClickListener(null)
        moreOptionButton?.setOnClickListener(null)
        fullscreenButton?.setOnClickListener(null)
        unlockButton?.setOnClickListener(null)
        speedPlaybackButton?.setOnClickListener(null)
        deviceRotateButton?.setOnClickListener(null)
        rewButton?.setOnClickListener(null)
        ffwdButton?.setOnClickListener(null)

        seekHandler.removeCallbacksAndMessages(null)
        seekChipState.value = null
        playerComposeView.removeView(seekChipView)
        seekChipView.disposeComposition()

        longPressHandler.removeCallbacksAndMessages(null)
        if (isLongPressActive) releaseLongPress()
        playerComposeView.removeView(longPressChipView)
        longPressChipView.disposeComposition()

        zoomChipHandler.removeCallbacksAndMessages(null)
        zoomChipState.value = null
        playerComposeView.removeView(zoomChipView)
        zoomChipView.disposeComposition()

        sliderHideHandler.removeCallbacksAndMessages(null)
        brightnessSliderState.value = null
        volumeSliderState.value = null
        brightnessSliderView?.let {
            playerComposeView.removeView(it)
            it.disposeComposition()
        }
        brightnessSliderView = null
        volumeSliderView?.let {
            playerComposeView.removeView(it)
            it.disposeComposition()
        }
        volumeSliderView = null

        playerComposeView.videoSurfaceView?.removeOnLayoutChangeListener(videoLayoutChangeListener)
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

        // Matches the system default long-press threshold (~500ms), giving brightness/volume swipes
        // a larger window to be recognized before the speed-change gesture fires.
        private val LONG_PRESS_TIMEOUT = 500.milliseconds

        // Slider lingers briefly after touch-up so the user can read the final value.
        private val SLIDER_HIDE_DELAY = 800.milliseconds

        // Zoom chip lingers briefly after the pinch ends so the user can read the final level.
        private val ZOOM_CHIP_HIDE_DELAY = 800.milliseconds
        private const val SLIDER_SIDE_MARGIN_PORT_DP = 30
        private const val SLIDER_SIDE_MARGIN_LAND_DP = 60

        // Empirically tuned: a half-screen swipe (~45% of screen height) maps to ~100% range
        // change. Comparable to YouTube's gesture sensitivity on a typical phone screen.
        private const val GESTURE_SCROLL_SENSITIVITY = 2.5f

        // Android's Settings.System.SCREEN_BRIGHTNESS uses a 0–255 integer scale.
        private const val SYSTEM_BRIGHTNESS_MAX = 255f

        // Android returns -1f when the window is using the system default brightness;
        // 0.5f (50%) is used as a reasonable starting point for the gesture.
        private const val DEFAULT_BRIGHTNESS_FALLBACK = 0.5f

        // Used when AudioManager is unavailable; starts the volume gesture at 50%.
        private const val DEFAULT_VOLUME_FALLBACK = 0.5f
    }
}
