package mega.privacy.android.app.presentation.videoplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Matrix
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.SpeedSelectedPopup
import mega.privacy.android.app.mediaplayer.VideoOptionPopup
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoOptionItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class VideoPlayerController(
    private val context: Context,
    private val uiState: VideoPlayerUiState,
    container: ViewGroup,
    private val isShowSubtitleIcon: Boolean,
    private val updateRepeatToggleMode: () -> Unit,
    private val updateIsVideoOptionPopupShown: (Boolean) -> Unit,
    private val updateIsSpeedPopupShown: (Boolean) -> Unit,
    private val speedPlaybackItemSelected: (SpeedPlaybackItem) -> Unit,
    private val updateLockStatus: (Boolean) -> Unit,
    private val showSubtitleDialog: () -> Unit,
    private val fullscreenClickedCallback: (Boolean) -> Unit,
    private val lockStateChanged: (lock: Boolean) -> Unit,
    private val playQueueButtonClicked: () -> Unit,
    private val playerViewClicked: () -> Unit,
    private val captureScreenShot: () -> Unit,
) {
    private val playQueueButton = container.findViewById<ImageButton>(R.id.playlist)
    private val trackName = container.findViewById<TextView>(R.id.track_name)
    private val repeatToggleButton = container.findViewById<ImageButton>(R.id.repeat_toggle)
    private val playerComposeView = container.findViewById<PlayerView>(R.id.player_compose_view)
    private val moreOptionButton = container.findViewById<ImageButton>(R.id.more_option)
    private val videoOptionPopup = container.findViewById<ComposeView>(R.id.video_option_popup)
    private val screenshotButton = container.findViewById<ImageButton>(R.id.image_screenshot)
    private val fullscreenButton = container.findViewById<ImageButton>(R.id.full_screen)
    private val lockButton = container.findViewById<ImageButton>(R.id.image_button_lock)
    private val controllerView = container.findViewById<View>(R.id.layout_player)
    private val unlockView = container.findViewById<View>(R.id.layout_unlock)
    private val unlockButton = container.findViewById<ImageButton>(R.id.image_button_unlock)
    private val speedPlaybackButton = container.findViewById<ImageButton>(R.id.speed_playback)
    private val speedPlaybackPopup = container.findViewById<ComposeView>(R.id.speed_playback_popup)
    private val subtitleButton = container.findViewById<ImageButton>(R.id.subtitle)

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var zoomLevel = 1.0f
    private val maxZoom = 5.0f
    private var translationX = 0f
    private var translationY = 0f

    private var isSpeedPopupShown = mutableStateOf(uiState.isSpeedPopupShown)
    private var isVideoOptionPopupShown = mutableStateOf(uiState.isVideoOptionPopupShown)
    private var currentSpeedPlayback = mutableStateOf(uiState.currentSpeedPlayback)
    private var isFullscreen = mutableStateOf(uiState.isFullscreen)

    init {
        setupRepeatToggleButton(uiState.repeatToggleMode)
        setupMoreOptionButton()
        setupVideoPlayQueueButton(uiState.items.size)
        screenshotButton.setOnClickListener {
            screenshotButtonClicked()
        }
        setupFullscreen(uiState.isFullscreen)

        lockButton.setOnClickListener {
            updateLockState(true)
        }

        unlockButton.setOnClickListener {
            updateLockState(false)
        }
        setupSpeedPlaybackButton()
        setupGestures()
        setupSubtitleButton()
    }

    /**
     * Setup video play queue button.
     *
     * @param size the video play queue size
     */
    private fun setupVideoPlayQueueButton(size: Int) {
        togglePlayQueueEnabled(size)
        playQueueButton.setOnClickListener {
            playQueueButtonClicked()
        }
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
     * Toggle the playlist button.
     *
     * @param itemSize the item size
     */
    internal fun togglePlayQueueEnabled(itemSize: Int) {
        playQueueButton.visibility =
            if (itemSize > SINGLE_PLAYLIST_SIZE)
                View.VISIBLE
            else
                View.INVISIBLE
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
            val videoOptions = remember(isFullscreen.value) {
                listOf(
                    VideoOptionItem.VIDEO_OPTION_SNAPSHOT,
                    VideoOptionItem.VIDEO_OPTION_LOCK,
                    if (isFullscreen.value) {
                        VideoOptionItem.VIDEO_OPTION_ORIGINAL
                    } else {
                        VideoOptionItem.VIDEO_OPTION_ZOOM_TO_FILL
                    }
                )
            }
            val orientation = LocalConfiguration.current.orientation

            VideoOptionPopup(
                items = videoOptions,
                isShown = isVideoOptionPopupShown.value && orientation == ORIENTATION_PORTRAIT,
                onDismissRequest = {
                    updateIsVideoOptionPopupShown(false)
                    isVideoOptionPopupShown.value = false
                }
            ) { videOption ->
                when (videOption) {
                    VideoOptionItem.VIDEO_OPTION_SNAPSHOT -> screenshotButtonClicked()
                    VideoOptionItem.VIDEO_OPTION_LOCK -> updateLockState(true)
                    else -> {
                        isFullscreen.value = !isFullscreen.value
                        fullscreenClickedCallback(isFullscreen.value)
                    }
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

    private fun screenshotButtonClicked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasPermissions(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            val activity = context as? Activity
            if (activity == null) {
                Timber.e("Context is not an activity")
                return
            }
            requestPermission(
                activity,
                REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            captureScreenShot()
        }
    }

    internal fun setupFullscreen(isFullScreen: Boolean) {
        updateFullscreenButtonIcon(isFullScreen)
        fullscreenButton.setOnClickListener {
            isFullscreen.value = !isFullscreen.value
            fullscreenClickedCallback(isFullscreen.value)
        }
    }

    internal fun updateFullscreenButtonIcon(isFullScreen: Boolean) =
        fullscreenButton.setImageResource(
            if (isFullScreen) {
                R.drawable.ic_original
            } else {
                R.drawable.ic_full_screen
            }
        )

    private fun updateLockState(isLock: Boolean) {
        controllerView.isVisible = !isLock
        unlockView.isVisible = isLock
        lockStateChanged(isLock)
        updateLockStatus(isLock)
    }

    internal fun updateLockView(isLock: Boolean) {
        controllerView.isVisible = !isLock
        unlockView.isVisible = isLock
    }

    internal fun updateSpeedPlaybackButtonIcon(@DrawableRes icon: Int) {
        speedPlaybackButton.setImageResource(icon)
    }

    private fun setupSpeedPlaybackButton() {
        initSpeedPlaybackPopup(speedPlaybackPopup)
        speedPlaybackButton.setImageResource(uiState.currentSpeedPlayback.iconId)
        speedPlaybackButton.setOnClickListener {
            updateIsSpeedPopupShown(true)
            isSpeedPopupShown.value = true
        }
    }

    private fun initSpeedPlaybackPopup(composeView: ComposeView) {
        composeView.setupComposeView(context) {
            SpeedSelectedPopup(
                items = SpeedPlaybackItem.entries,
                isShown = isSpeedPopupShown.value,
                currentPlaybackSpeed = currentSpeedPlayback.value,
                onDismissRequest = {
                    updateIsSpeedPopupShown(false)
                    isSpeedPopupShown.value = false
                }
            ) { speedPlaybackItem ->
                speedPlaybackItemSelected(speedPlaybackItem)
                updateIsSpeedPopupShown(false)
                currentSpeedPlayback.value = speedPlaybackItem
                isSpeedPopupShown.value = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (!uiState.isLocked) {
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
                    if (zoomLevel > 1 && !uiState.isLocked) {
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
        (playerComposeView?.videoSurfaceView as? TextureView)?.let { textureView ->
            val matrix = Matrix()
            matrix.postScale(zoomLevel, zoomLevel, textureView.width / 2f, textureView.height / 2f)
            matrix.postTranslate(translationX, translationY)
            textureView.setTransform(matrix)
            if (uiState.mediaPlaybackState == MediaPlaybackState.Paused) {
                textureView.invalidate()
                textureView.requestLayout()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun enforceBoundaries() {
        playerComposeView?.videoSurfaceView?.let { textureView ->
            val maxTranslationX = (zoomLevel - 1) * textureView.width / 2
            val maxTranslationY = (zoomLevel - 1) * textureView.height / 2

            translationX = translationX.coerceIn(-maxTranslationX, maxTranslationX)
            translationY = translationY.coerceIn(-maxTranslationY, maxTranslationY)
        }
    }

    private fun setupSubtitleButton() {
        subtitleButton.isVisible = isShowSubtitleIcon
        updateSubtitleButtonUI(uiState.subtitleSelectedStatus)
        subtitleButton.setOnClickListener {
            showSubtitleDialog()
        }
    }

    internal fun updateSubtitleButtonUI(status: SubtitleSelectedStatus) =
        subtitleButton.setImageResource(
            if (status == SubtitleSelectedStatus.Off) {
                R.drawable.ic_subtitles_disable
            } else {
                R.drawable.ic_subtitles_enable
            }
        )
}