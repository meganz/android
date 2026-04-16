package mega.privacy.android.app.presentation.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.VideoOptionRevampPopup
import mega.privacy.android.app.mediaplayer.model.RevampVideoOptionItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import timber.log.Timber

class VideoPlayerController(
    private val context: Context,
    private val uiState: VideoPlayerUiState,
    container: ViewGroup,
    private val isShowSubtitleIcon: Boolean,
    private val updateRepeatToggleMode: () -> Unit,
    private val updateIsVideoOptionPopupShown: (Boolean) -> Unit,
    private val updateIsSpeedOptionsShown: (Boolean) -> Unit,
    private val updateLockStatus: (Boolean) -> Unit,
    private val showSubtitleDialog: () -> Unit,
    private val fullscreenClickedCallback: (Boolean) -> Unit,
    private val lockStateChanged: (lock: Boolean) -> Unit,
    private val playQueueButtonClicked: () -> Unit,
    private val playerViewClicked: () -> Unit,
    private val onSnapshotSelected: () -> Unit,
) {
    private val trackName = container.findViewById<TextView>(R.id.track_name)
    private val repeatToggleButton = container.findViewById<ImageButton>(R.id.repeat_toggle)
    private val playerComposeView = container.findViewById<PlayerView>(R.id.player_compose_view)
    private val moreOptionButton = container.findViewById<ImageButton>(R.id.more_option)
    private val videoOptionPopup = container.findViewById<ComposeView>(R.id.video_option_popup)
    private val fullscreenButton = container.findViewById<ImageButton>(R.id.full_screen)
    private val controllerView = container.findViewById<View>(R.id.layout_player)
    private val unlockView = container.findViewById<View>(R.id.layout_unlock)
    private val unlockButton = container.findViewById<ImageButton>(R.id.image_button_unlock)
    private val speedPlaybackButton = container.findViewById<TextView>(R.id.speed_playback)
    private val deviceRotateButton = container.findViewById<ImageButton>(R.id.device_rotated)

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var zoomLevel = 1.0f
    private val maxZoom = 5.0f
    private var translationX = 0f
    private var translationY = 0f

    private var isVideoOptionPopupShown = mutableStateOf(uiState.isVideoOptionPopupShown)
    private var isFullscreen = mutableStateOf(uiState.isFullscreen)
    private var playbackState = uiState.mediaPlaybackState
    private var isLocked = mutableStateOf(uiState.isLocked)
    private var playQueueInOverflowMenu = mutableStateOf(uiState.items.size > SINGLE_PLAYLIST_SIZE)

    init {
        setupRepeatToggleButton(uiState.repeatToggleMode)
        setupMoreOptionButton()
        updatePlayQueueOverflowMenuItems(uiState.items.size)
        setupFullscreen(uiState.isFullscreen)
        setupLockButton()
        setupSpeedPlaybackButton()
        setupGestures()
        setupDeviceRotateButton()
    }

    /**
     * Setup the repeat toggle button
     *
     * @param defaultRepeatToggleMode the default RepeatToggleMode
     */
    private fun setupRepeatToggleButton(defaultRepeatToggleMode: RepeatToggleMode) {
        repeatToggleButton.isVisible = true
        updateRepeatToggleButtonUI(context, defaultRepeatToggleMode)
        repeatToggleButton.setOnClickListener {
            updateRepeatToggleMode()
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
     * Updates whether [RevampVideoOptionItem.Playlist] appears in the overflow menu (same rule as legacy toolbar playlist).
     */
    internal fun updatePlayQueueOverflowMenuItems(itemSize: Int) {
        playQueueInOverflowMenu.value = itemSize > SINGLE_PLAYLIST_SIZE
    }

    private fun setupMoreOptionButton() {
        initVideoOptionPopup(videoOptionPopup)
        moreOptionButton.setOnClickListener {
            updateIsVideoOptionPopupShown(true)
            isVideoOptionPopupShown.value = true
        }
    }

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    internal fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }

    private fun initVideoOptionPopup(composeView: ComposeView) {
        composeView.setupComposeView(context) {
            val videoOptions = remember(playQueueInOverflowMenu.value, isShowSubtitleIcon) {
                buildList {
                    add(RevampVideoOptionItem.Snapshot)
                    if (isShowSubtitleIcon) {
                        add(RevampVideoOptionItem.Subtitle)
                    }
                    if (playQueueInOverflowMenu.value) {
                        add(RevampVideoOptionItem.Playlist)
                    }
                    add(RevampVideoOptionItem.Lock)
                }
            }

            VideoOptionRevampPopup(
                items = videoOptions,
                isShown = isVideoOptionPopupShown.value,
                onDismissRequest = {
                    updateIsVideoOptionPopupShown(false)
                    isVideoOptionPopupShown.value = false
                }
            ) { option ->
                when (option) {
                    RevampVideoOptionItem.Snapshot -> onSnapshotSelected()
                    RevampVideoOptionItem.Subtitle -> showSubtitleDialog()
                    RevampVideoOptionItem.Playlist -> playQueueButtonClicked()
                    RevampVideoOptionItem.Lock -> updateLockState(true)
                }
                updateIsVideoOptionPopupShown(false)
                isVideoOptionPopupShown.value = false
            }
        }
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
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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

                @OptIn(UnstableApi::class)
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    playerViewClicked()
                    return true
                }
            })

        playerComposeView.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event)
            gestureDetector?.onTouchEvent(event)
            true
        }
    }

    @OptIn(UnstableApi::class)
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

    @OptIn(UnstableApi::class)
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

        playerComposeView?.setOnTouchListener(null)

        videoOptionPopup.disposeComposition()

        scaleGestureDetector = null
        gestureDetector = null

        isVideoOptionPopupShown.value = false
    }
}
