package mega.privacy.android.app.mediaplayer

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.isVisible
import mega.privacy.android.app.utils.getScreenHeight
import mega.privacy.android.app.utils.getScreenWidth
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.jetbrains.anko.configuration
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The Fragment for the video player
 */
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {
    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private var playerViewHolder: VideoPlayerViewHolder? = null

    private lateinit var binding: FragmentVideoPlayerBinding

    private val viewModel: VideoPlayerViewModel by activityViewModels()

    private val videoPlayerActivity by lazy {
        activity as? VideoPlayerActivity
    }

    private var playlistObserved = false

    private var delayHideToolbarCanceled = false

    private var videoPlayerView: StyledPlayerView? = null

    private var toolbarVisible = false

    private var retryFailedDialog: AlertDialog? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updateLoadingAnimation(state)
            // The subtitle button is enable after the video is in buffering state
            playerViewHolder?.subtitleButtonEnable(state >= Player.STATE_BUFFERING)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            if (view != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                viewModel.updateCurrentMediaId(mediaItem?.mediaId)
            }
        }
    }

    private val selectSubtitleFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    (result.data?.serializable(
                        INTENT_KEY_SUBTITLE_FILE_INFO
                    ) as? SubtitleFileInfo?).let { info ->
                        viewModel.onAddSubtitleFile(info)
                        if (info?.url == null) {
                            showAddingSubtitleFailedMessage()
                        }
                    }
                }

                Activity.RESULT_CANCELED ->
                    viewModel.onAddSubtitleFile(null)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentVideoPlayerBinding.inflate(inflater, container, false).let {
        binding = it
        playerViewHolder = VideoPlayerViewHolder(binding)
        binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoPlayerActivity?.updateToolbarTitleBasedOnOrientation(viewModel.metadataState.value)
        observeFlow()
    }

    override fun onResume() {
        super.onResume()
        setupPlayer()

        if (!toolbarVisible && !viewModel.isPlayingReverted()) {
            showToolbar()
            delayHideToolbar()
        } else {
            hideToolbar()
            playerViewHolder?.hideController()
        }
        // According to the value of isPlayingReverted to confirm whether revert the video to play
        if (viewModel.isPlayingReverted()) {
            mediaPlayerGateway.setPlayWhenReady(true)
            viewModel.setPlayingReverted(false)
        }

        videoPlayerActivity?.setDraggable(!viewModel.screenLockState.value)
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateAddSubtitleState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
        playerViewHolder = null
        mediaPlayerGateway.removeListener(playerListener)
    }

    private fun observeFlow() {
        if (view != null) {
            with(viewModel) {
                viewLifecycleOwner.collectFlow(metadataState) { metadata ->
                    playerViewHolder?.displayMetadata(metadata)
                }

                viewLifecycleOwner.collectFlow(playerControllerPaddingState) { (left, right, bottom) ->
                    updatePlayerControllerPadding(left = left, right = right, bottom = bottom)
                }

                if (!playlistObserved) {
                    playlistObserved = true
                    viewLifecycleOwner.collectFlow(playlistItemsState) { info ->
                        Timber.d("MediaPlayerService observed playlist ${info.first.size} items")
                        playerViewHolder?.togglePlaylistEnabled(info.first)
                    }

                    viewLifecycleOwner.collectFlow(retryState) { isRetry ->
                        isRetry?.let {
                            mediaPlayerGateway.mediaPlayerRetry(it)
                            when {
                                !it && retryFailedDialog == null -> {
                                    retryFailedDialog =
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setCancelable(false)
                                            .setMessage(
                                                getString(
                                                    if (Util.isOnline(requireContext()))
                                                        R.string.error_fail_to_open_file_general
                                                    else
                                                        R.string.error_fail_to_open_file_no_network
                                                )
                                            )
                                            .setPositiveButton(getString(R.string.general_ok)) { _, _ ->
                                                mediaPlayerGateway.playerStop()
                                                requireActivity().finish()
                                            }
                                            .show()
                                }

                                it -> {
                                    retryFailedDialog?.dismiss()
                                    retryFailedDialog = null
                                }
                            }
                        }
                    }

                    viewLifecycleOwner.collectFlow(mediaPlaybackState) { isPaused ->
                        // The keepScreenOn is true when the video is playing, otherwise it's false.
                        videoPlayerView?.keepScreenOn = !isPaused
                    }
                }

                viewLifecycleOwner.collectFlow(subtitleDisplayState) { state ->
                    playerViewHolder?.updateSubtitleButtonUI(state.isSubtitleShown)
                    if (!state.isSubtitleDialogShown) {
                        delayHideToolbar()
                    }
                    if (state.isSubtitleShown) {
                        if (state.isAddSubtitle) {
                            state.subtitleFileInfo?.url?.let {
                                addSubtitle(it)
                            }
                        }
                        mediaPlayerGateway.showSubtitle()
                    } else {
                        mediaPlayerGateway.hideSubtitle()
                    }
                }
            }
        }
    }

    private fun setupPlayer() {
        playerViewHolder?.let { viewHolder ->
            videoPlayerView = viewHolder.binding.playerView
            videoPlayerView?.keepScreenOn = !viewModel.mediaPlaybackState.value
            with(viewHolder) {
                setTrackNameVisible(activity?.configuration?.orientation != ORIENTATION_LANDSCAPE)

                // we need setup control buttons again, because reset player would reset PlayerControlView
                setupPlaylistButton(viewModel.getPlaylistItems()) {
                    videoPlayerActivity?.setDraggable(false)
                    findNavController().let {
                        if (it.currentDestination?.id == R.id.video_main_player) {
                            it.navigate(VideoPlayerFragmentDirections.actionVideoPlayerToPlaylist())
                        }
                    }
                }

                setupLockUI(viewModel.screenLockState.value) { isLock ->
                    viewModel.updateLockStatus(isLock)
                    if (isLock) {
                        delayHideWhenLocked()
                        viewModel.sendScreenLockedEvent()
                    } else {
                        viewModel.sendScreenUnlockedEvent()
                    }
                }

                initAddSubtitleDialog(viewHolder.binding.addSubtitleDialog)

                viewLifecycleOwner.lifecycleScope.launch {
                    setupSubtitleButton(
                        // Add feature flag, and will be removed after the feature is finished.
                        isShow = showSubtitleIcon(),
                        isSubtitleShown = viewModel.subtitleDisplayState.value.isSubtitleShown
                    ) {
                        viewModel.showAddSubtitleDialog()
                    }
                }

                setupScreenshotButton {
                    val rootPath =
                        getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
                    val screenshotsFolderPath =
                        "${rootPath}${File.separator}${MEGA_SCREENSHOTS_FOLDER_NAME}${File.separator}"
                    viewHolder.binding.playerView.videoSurfaceView?.let { view ->
                        viewModel.screenshotWhenVideoPlaying(
                            rootFolderPath = screenshotsFolderPath,
                            captureView = view
                        ) { bitmap ->
                            viewModel.sendSnapshotButtonClickedEvent()
                            requireActivity().runOnUiThread {
                                showCaptureScreenshotAnimation(
                                    view = binding.screenshotScaleAnimationView,
                                    layout = binding.screenshotScaleAnimationLayout,
                                    bitmap = bitmap
                                )
                                videoPlayerActivity?.showSnackBarForVideoPlayer(
                                    getString(R.string.media_player_video_snackbar_screenshot_saved)
                                )
                            }
                        }
                    }
                }
            }

            setupPlayerView(viewHolder.binding.playerView)

            initRepeatToggleButtonForVideo(viewHolder)
        }
    }

    private fun showCaptureScreenshotAnimation(
        view: ImageView,
        layout: LinearLayout,
        bitmap: Bitmap,
    ) {
        if (!layout.isVisible()) {
            layout.isVisible = true
        }
        val screenWidth = requireActivity().getScreenWidth()
        val screenHeight = requireActivity().getScreenHeight()
        val currentOrientation = activity?.configuration?.orientation

        // Re-compute the size based on screen size
        val (width, height) = if (currentOrientation == ORIENTATION_LANDSCAPE
            && bitmap.height > bitmap.width
        ) {
            (screenHeight * bitmap.width / bitmap.height) to screenHeight
        } else {
            screenWidth to (screenWidth * bitmap.height / bitmap.width)
        }
        // Resize the bitmap based on the screen size to avoid the bitmap size being greater than the screen size
        val resizeBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        view.setImageBitmap(resizeBitmap)

        val scaleY = if (currentOrientation == ORIENTATION_LANDSCAPE) {
            SCREENSHOT_SCALE_LANDSCAPE
        } else {
            SCREENSHOT_SCALE_PORTRAIT
        }
        // If the width of the screenshot is too small when the orientation is landscape,
        // use 0.7 for  the value of "relateX" to avoid the screenshot animation cannot be displayed as completed
        val relateX =
            if (currentOrientation == ORIENTATION_LANDSCAPE && width * scaleY / screenWidth < 0.15) {
                SCREENSHOT_RELATE_X_70
            } else {
                SCREENSHOT_RELATE_X_90
            }

        val anim: Animation = ScaleAnimation(
            SCREENSHOT_SCALE_ORIGINAL,
            scaleY,  // Start and end values for the X axis scaling
            SCREENSHOT_SCALE_ORIGINAL,
            scaleY,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_PARENT,
            relateX,  // Pivot point of X scaling
            Animation.RELATIVE_TO_PARENT,
            SCREENSHOT_RELATE_Y // Pivot point of Y scaling
        )

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                view.postDelayed({
                    view.setImageDrawable(null)
                    layout.isVisible = false
                    layout.clearAnimation()
                    bitmap.recycle()
                    resizeBitmap.recycle()
                }, SCREENSHOT_DURATION)
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })
        anim.fillAfter = true
        anim.duration = ANIMATION_DURATION
        layout.startAnimation(anim)
    }

    private fun initRepeatToggleButtonForVideo(viewHolder: VideoPlayerViewHolder) {
        val defaultRepeatMode = viewModel.getVideoRepeatMode()

        mediaPlayerGateway.setRepeatToggleMode(
            if (defaultRepeatMode == RepeatToggleMode.REPEAT_NONE) {
                RepeatToggleMode.REPEAT_NONE
            } else {
                RepeatToggleMode.REPEAT_ONE
            }
        )
        viewHolder.setupRepeatToggleButton(
            requireContext(),
            defaultRepeatMode
        ) { repeatToggleButton ->
            viewModel.videoRepeatToggleMode().let { repeatToggleMode ->
                if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                    mediaPlayerGateway.setRepeatToggleMode(RepeatToggleMode.REPEAT_ONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.teal_300))
                    viewModel.sendLoopButtonEnabledEvent()
                } else {
                    mediaPlayerGateway.setRepeatToggleMode(RepeatToggleMode.REPEAT_NONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.white))
                }
            }
        }
    }

    private fun setupPlayerView(
        playerView: StyledPlayerView,
    ) {
        mediaPlayerGateway.setupPlayerView(
            playerView = playerView,
            controllerHideOnTouch = true,
            isAudioPlayer = false,
            showShuffleButton = false,
        )

        playerView.setOnClickListener {
            if (toolbarVisible) {
                hideToolbar()
                playerView.hideController()
            } else {
                delayHideToolbarCanceled = true
                showToolbar()
                playerView.showController()
                if (viewModel.screenLockState.value) {
                    delayHideToolbar()
                }
            }
        }
        updateLoadingAnimation(mediaPlayerGateway.getPlaybackState())
        mediaPlayerGateway.addPlayerListener(playerListener)
    }

    private fun updateLoadingAnimation(@Player.State playbackState: Int?) =
        playerViewHolder?.updateLoadingAnimation(playbackState)

    private fun delayHideToolbar() {
        delayHideToolbarCanceled = false

        RunOnUIThreadUtils.runDelay(Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed && !delayHideToolbarCanceled) {
                hideToolbar()
                playerViewHolder?.hideController()
            }
        }
    }

    private fun delayHideWhenLocked() {
        RunOnUIThreadUtils.runDelay(Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (viewModel.screenLockState.value) {
                hideToolbar()
                playerViewHolder?.hideController()
            }
        }
    }

    private fun hideToolbar(animate: Boolean = true) {
        toolbarVisible = false
        videoPlayerActivity?.hideToolbar(animate)
    }

    private fun showToolbar() {
        toolbarVisible = true
        if (viewModel.screenLockState.value) {
            videoPlayerActivity?.showSystemUI()
        } else {
            videoPlayerActivity?.showToolbar()
        }
    }

    /**
     * Run the enter animation
     *
     * @param dragToExit DragToExitSupport
     */
    fun runEnterAnimation(dragToExit: DragToExitSupport) {
        val binding = playerViewHolder?.binding ?: return

        dragToExit.runEnterAnimation(requireActivity().intent, binding.playerView) {
            if (it) {
                updateViewForAnimation()
            } else if (isResumed) {
                showToolbar()
                playerViewHolder?.showController()

                binding.root.setBackgroundColor(Color.BLACK)

                delayHideToolbar()
            }
        }
    }

    /**
     * On drag activated
     *
     * @param dragToExit DragToExitSupport
     * @param activated true is activated, otherwise is false
     */
    fun onDragActivated(dragToExit: DragToExitSupport, activated: Boolean) {
        if (activated) {
            delayHideToolbarCanceled = true
            updateViewForAnimation()

            playerViewHolder?.binding?.playerView?.videoSurfaceView.let { videoSurfaceView ->
                dragToExit.setCurrentView(videoSurfaceView)
            }
        } else {
            RunOnUIThreadUtils.runDelay(300L) {
                playerViewHolder?.binding?.root?.setBackgroundColor(Color.BLACK)
            }
        }
    }

    private fun updateViewForAnimation() {
        hideToolbar(animate = false)
        playerViewHolder?.hideController()

        playerViewHolder?.binding?.root?.setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Init the add subtitle dialog
     *
     * @param composeView compose view
     */
    private fun initAddSubtitleDialog(composeView: ComposeView) {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                if (viewModel.subtitleDisplayState.collectAsState().value.isSubtitleDialogShown) {
                    AddSubtitleDialog(
                        selectOptionState = viewModel.selectOptionState,
                        matchedSubtitleFileUpdate = {
                            viewModel.getMatchedSubtitleFileInfoForPlayingItem()
                        },
                        subtitleFileName = viewModel.subtitleInfoByAddSubtitles?.name,
                        onOffClicked = {
                            viewModel.onOffItemClicked()
                        },
                        onAddedSubtitleClicked = {
                            viewModel.onAddedSubtitleOptionClicked()
                        },
                        onAutoMatch = { info ->
                            if (info.url == null) {
                                showAddingSubtitleFailedMessage()
                            }
                            viewModel.onAutoMatchItemClicked(info)
                        },
                        onToSelectSubtitle = {
                            viewModel.sendOpenSelectSubtitlePageEvent()
                            selectSubtitleFileActivityLauncher.launch(
                                Intent(
                                    requireActivity(),
                                    SelectSubtitleFileActivity::class.java
                                ).apply {
                                    putExtra(
                                        INTENT_KEY_SUBTITLE_FILE_ID,
                                        viewModel.subtitleDisplayState.value.subtitleFileInfo?.id
                                    )
                                }
                            )
                        }) {
                        // onDismissRequest
                        viewModel.onDismissRequest()
                    }
                }
            }
        }
    }

    private fun addSubtitle(subtitleFileUrl: String) {
        if (!mediaPlayerGateway.addSubtitle(subtitleFileUrl)) {
            showAddingSubtitleFailedMessage()
            viewModel.onAddSubtitleFile(info = null, isReset = true)
        }
        // Don't recreate play sources if the playlist is unavailable,
        // to avoid the subtitle not working for a single item played.
        if (activity?.intent?.getBooleanExtra(
                Constants.INTENT_EXTRA_KEY_IS_PLAYLIST,
                true
            ) == true
        ) {
            viewModel.playerSourcesState.value.let {
                viewModel.playSource(
                    MediaPlaySources(
                        it.mediaItems,
                        viewModel.getPlayingPosition(),
                        null
                    )
                )
            }
        }
    }

    private fun showAddingSubtitleFailedMessage() {
        (activity as? VideoPlayerActivity)?.showSnackBarForVideoPlayer(
            getString(R.string.media_player_video_message_adding_subtitle_failed)
        )
    }

    /**
     * Show the subtitle icon when the adapterType is not OFFLINE_ADAPTER
     */
    private fun showSubtitleIcon() =
        activity?.intent?.getIntExtra(
            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
            Constants.INVALID_VALUE
        ) != Constants.OFFLINE_ADAPTER

    /**
     * Update the player controls view padding to adapt the system UI displayed.
     *
     * @param left padding left
     * @param right padding right
     * @param bottom padding bottom
     */
    private fun updatePlayerControllerPadding(left: Int, right: Int, bottom: Int) {
        binding.root.findViewById<ConstraintLayout>(R.id.controls_view)
            .updatePadding(left = left, top = 0, right = right, bottom = bottom)
    }

    companion object {
        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "MEGA Screenshots"
        private const val SCREENSHOT_SCALE_ORIGINAL: Float = 1F
        private const val SCREENSHOT_SCALE_PORTRAIT: Float = 0.4F
        private const val SCREENSHOT_SCALE_LANDSCAPE: Float = 0.3F
        private const val SCREENSHOT_RELATE_X_70: Float = 0.7F
        private const val SCREENSHOT_RELATE_X_90: Float = 0.9F
        private const val SCREENSHOT_RELATE_Y: Float = 0.75F
        private const val ANIMATION_DURATION: Long = 500
        private const val SCREENSHOT_DURATION: Long = 500

        /**
         * The intent key for passing subtitle file info
         */
        const val INTENT_KEY_SUBTITLE_FILE_INFO = "INTENT_KEY_SUBTITLE_FILE_INFO"

        /**
         * The intent key for passing subtitle file id
         */
        const val INTENT_KEY_SUBTITLE_FILE_ID = "INTENT_KEY_SUBTITLE_FILE_ID"
    }
}