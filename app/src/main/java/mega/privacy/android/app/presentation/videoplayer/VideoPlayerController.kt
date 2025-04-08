package mega.privacy.android.app.presentation.videoplayer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.SpeedSelectedPopup
import mega.privacy.android.app.mediaplayer.VideoOptionPopup
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoOptionItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.mobile.analytics.event.LoopButtonPressedEvent
import mega.privacy.mobile.analytics.event.SnapshotButtonPressedEvent
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class VideoPlayerController(
    private val context: Context,
    private val viewModel: VideoPlayerViewModel,
    container: ViewGroup,
    coroutineScope: CoroutineScope,
    private val fullscreenClickedCallback: () -> Unit,
    private val lockStateChanged: (lock: Boolean) -> Unit,
    private val playQueueButtonClicked: () -> Unit,
    private val captureScreenShotFinished: (Bitmap) -> Unit,
) : LifecycleEventObserver {
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

    private var sharingScope: CoroutineScope? = null

    init {
        coroutineScope.launch {
            viewModel.uiState.map { it.items }.distinctUntilChanged().collectLatest {
                if (it.isNotEmpty()) {
                    togglePlayQueueEnabled(it.size)
                }
            }
        }

        coroutineScope.launch {
            viewModel.uiState.map { it.isFullscreen }.distinctUntilChanged().collectLatest {
                updateFullscreenButtonIcon(it)
            }
        }

        coroutineScope.launch {
            viewModel.uiState.map { it.isLocked }.distinctUntilChanged().collectLatest {
                controllerView.isVisible = !it
                unlockView.isVisible = it
            }
        }

        coroutineScope.launch {
            viewModel.uiState.map { it.currentSpeedPlayback }.distinctUntilChanged().collectLatest {
                speedPlaybackButton.setImageResource(it.iconId)
            }
        }

        setupRepeatToggleButton(viewModel.uiState.value.repeatToggleMode)
        setupMoreOptionButton()
        setupVideoPlayQueueButton(viewModel.uiState.value.items.size)
        screenshotButton.setOnClickListener {
            screenshotButtonClicked()
        }
        setupFullscreen(viewModel.uiState.value.isFullscreen)

        lockButton.setOnClickListener {
            updateLockState(true)
        }

        unlockButton.setOnClickListener {
            updateLockState(false)
        }
        setupSpeedPlaybackButton()
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
            val repeatToggleMode =
                viewModel.uiState.value.repeatToggleMode.let { repeatToggleMode ->
                    if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                        Analytics.tracker.trackEvent(LoopButtonPressedEvent)
                        RepeatToggleMode.REPEAT_ONE

                    } else {
                        RepeatToggleMode.REPEAT_NONE
                    }
                }
            viewModel.setRepeatToggleModeForPlayer(repeatToggleMode)
        }
    }

    /**
     * Update repeat toggle button UI
     *
     * @param context Context
     * @param repeatToggleMode the current RepeatToggleMode
     */
    private fun updateRepeatToggleButtonUI(
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
    private fun togglePlayQueueEnabled(itemSize: Int) {
        playQueueButton.visibility =
            if (itemSize > SINGLE_PLAYLIST_SIZE)
                View.VISIBLE
            else
                View.INVISIBLE

    }

    private fun setupMoreOptionButton() {
        initVideoOptionPopup(videoOptionPopup)
        moreOptionButton.setOnClickListener {
            viewModel.updateIsVideoOptionPopupShown(true)
        }
    }

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    private fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }

    private fun initVideoOptionPopup(composeView: ComposeView) {
        composeView.setupComposeView(context) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val videoOptions = remember(state.isFullscreen) {
                listOf(
                    VideoOptionItem.VIDEO_OPTION_SNAPSHOT,
                    VideoOptionItem.VIDEO_OPTION_LOCK,
                    if (state.isFullscreen) {
                        VideoOptionItem.VIDEO_OPTION_ORIGINAL
                    } else {
                        VideoOptionItem.VIDEO_OPTION_ZOOM_TO_FILL
                    }
                )
            }
            VideoOptionPopup(
                items = videoOptions,
                isShown = state.isVideoOptionPopupShown,
                onDismissRequest = { viewModel.updateIsVideoOptionPopupShown(false) }
            ) { videOption ->
                when (videOption) {
                    VideoOptionItem.VIDEO_OPTION_SNAPSHOT -> screenshotButtonClicked()
                    VideoOptionItem.VIDEO_OPTION_LOCK -> updateLockState(true)
                    else -> fullscreenClickedCallback()
                }
                viewModel.updateIsVideoOptionPopupShown(false)
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
            fullscreenClickedCallback()
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
        viewModel.updateLockStatus(isLock)
    }

    private fun setupSpeedPlaybackButton() {
        initSpeedPlaybackPopup(speedPlaybackPopup)
        speedPlaybackButton.setImageResource(viewModel.uiState.value.currentSpeedPlayback.iconId)
        speedPlaybackButton.setOnClickListener {
            viewModel.updateIsSpeedPopupShown(true)
        }
    }

    private fun initSpeedPlaybackPopup(composeView: ComposeView) {
        composeView.setupComposeView(context) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            SpeedSelectedPopup(
                items = SpeedPlaybackItem.entries,
                isShown = state.isSpeedPopupShown,
                currentPlaybackSpeed = state.currentSpeedPlayback,
                onDismissRequest = { viewModel.updateIsSpeedPopupShown(false) }
            ) { speedPlaybackItem ->
                viewModel.updateCurrentSpeedPlaybackItem(speedPlaybackItem)
                viewModel.updateIsSpeedPopupShown(false)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun captureScreenShot() {
        val rootPath =
            getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
        playerComposeView.videoSurfaceView?.let { view ->
            viewModel.screenshotWhenVideoPlaying(rootPath, captureView = view) { bitmap ->
                Analytics.tracker.trackEvent(SnapshotButtonPressedEvent)
                captureScreenShotFinished(bitmap)
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreated(source)
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> return
        }
    }

    private fun onCreated(source: LifecycleOwner) {
        if (sharingScope == null) {
            sharingScope = source.lifecycleScope
            sharingScope?.launch {
                viewModel.uiState.map { it.metadata }.distinctUntilChanged()
                    .collectLatest { metadata ->
                        displayMetadata(metadata)
                    }
            }

            sharingScope?.launch {
                viewModel.uiState.map { it.items }.distinctUntilChanged()
                    .collectLatest {
                        togglePlayQueueEnabled(it.size)
                    }
            }

            sharingScope?.launch {
                viewModel.uiState.map { it.repeatToggleMode }.distinctUntilChanged()
                    .collectLatest {
                        updateRepeatToggleButtonUI(context, it)
                    }
            }
        }
    }

    /**
     * The onResume function is called when Lifecycle event ON_RESUME
     *
     */
    fun onResume() {
        playerComposeView.onResume()
    }

    /**
     * The onPause function is called when Lifecycle event ON_PAUSE
     */
    fun onPause() {
        playerComposeView.onPause()
    }

    /**
     * The onDestroy function is called when Lifecycle event ON_DESTROY
     */
    fun onDestroy() {
        sharingScope?.cancel()
        sharingScope = null
    }
}