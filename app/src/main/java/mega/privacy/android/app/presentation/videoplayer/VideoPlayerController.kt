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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedR
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

    // Seek indicator overlay — created programmatically so it stays visible even when
    // the player controller is auto-hidden.
    private lateinit var seekIndicatorView: ComposeView
    private val seekState = mutableStateOf<SeekIndicatorState?>(null)

    private val seekHandler = Handler(Looper.getMainLooper())
    private var accumulatedSeekMs = 0L
    private val hideSeekIndicatorRunnable = Runnable {
        accumulatedSeekMs = 0L
        seekState.value = null
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
        initSeekIndicatorOverlay()
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

    private fun initSeekIndicatorOverlay() {
        fun Int.toPx(): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()

        seekIndicatorView = ComposeView(context)
        seekIndicatorView.setupComposeView(context) { SeekIndicatorContent() }

        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val bottomMarginDp =
            if (isLandscape) SEEK_INDICATOR_BOTTOM_MARGIN_LAND_DP
            else SEEK_INDICATOR_BOTTOM_MARGIN_PORT_DP

        playerComposeView.addView(
            seekIndicatorView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).also {
                it.bottomMargin = bottomMarginDp.toPx()
            },
        )
    }

    private data class SeekIndicatorState(val seconds: Int, val isForward: Boolean)

    @Composable
    private fun SeekIndicatorContent() {
        val state = seekState.value ?: return
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x80000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            if (!state.isForward) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Regular.Solid.FastBackward),
                    contentDescription = null,
                    tint = IconColor.Primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            MegaText(
                text = pluralStringResource(
                    SharedR.plurals.video_player_seek_seconds,
                    state.seconds,
                    state.seconds
                ),
                textColor = TextColor.Primary,
                style = AppTheme.typography.labelLarge
            )
            if (state.isForward) {
                Spacer(modifier = Modifier.width(8.dp))
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Regular.Solid.FastForward),
                    contentDescription = null,
                    tint = IconColor.Primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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
        seekState.value = SeekIndicatorState(
            seconds = absoluteSeconds,
            isForward = accumulatedSeekMs > 0,
        )

        seekHandler.removeCallbacks(hideSeekIndicatorRunnable)
        seekHandler.postDelayed(
            hideSeekIndicatorRunnable,
            SEEK_INDICATOR_HIDE_DELAY.inWholeMilliseconds
        )
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
            scaleGestureDetector?.onTouchEvent(event)
            gestureDetector?.onTouchEvent(event)
            true
        }
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

        seekHandler.removeCallbacks(hideSeekIndicatorRunnable)
        seekState.value = null
        playerComposeView.removeView(seekIndicatorView)

        playerComposeView?.setOnTouchListener(null)
        scaleGestureDetector = null
        gestureDetector = null
    }

    companion object {
        private val SEEK_STEP = 15.seconds
        private val SEEK_INDICATOR_HIDE_DELAY = 3.seconds

        // Portrait: positions overlay above the controller bar (~128 dp tall, 30 dp margin).
        private const val SEEK_INDICATOR_BOTTOM_MARGIN_PORT_DP = 145

        // Landscape: centers overlay between the timebar (~75 dp from bottom) and the play/pause button (screen center).
        private const val SEEK_INDICATOR_BOTTOM_MARGIN_LAND_DP = 110
    }
}
