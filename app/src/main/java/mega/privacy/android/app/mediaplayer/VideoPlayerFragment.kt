package mega.privacy.android.app.mediaplayer

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.IBinder
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
import androidx.core.view.isVisible
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
import mega.privacy.android.app.constants.SettingsConstants.KEY_VIDEO_REPEAT_MODE
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.isVisible
import org.jetbrains.anko.configuration
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.io.File

/**
 * The Fragment for the video player
 */
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {
    private var playerViewHolder: VideoPlayerViewHolder? = null

    private lateinit var binding: FragmentVideoPlayerBinding

    private val viewModel: VideoPlayerViewModel by activityViewModels()

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var serviceViewModelGateway: PlayerServiceViewModelGateway? = null

    private var playlistObserved = false

    private var delayHideToolbarCanceled = false

    private var videoPlayerView: StyledPlayerView? = null
    private var playbackPositionDialog: Dialog? = null

    private var toolbarVisible = true

    private var retryFailedDialog: AlertDialog? = null

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
            serviceViewModelGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                serviceViewModelGateway = service.playerServiceViewModelGateway

                setupPlayer()
                observeFlow()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (isResumed) {
                updateLoadingAnimation(state)
            }
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
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onAddSubtitleFile(
                    result.data?.serializable(
                        INTENT_KEY_SUBTITLE_FILE_INFO
                    )
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.bindService(
            Intent(
                requireContext(),
                VideoPlayerService::class.java
            ).putExtra(Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false),
            connection,
            Context.BIND_AUTO_CREATE
        )
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

        observeFlow()
    }

    override fun onResume() {
        super.onResume()

        if (serviceGateway != null && serviceViewModelGateway != null) {
            setupPlayer()
        }

        if (!toolbarVisible) {
            showToolbar()
            delayHideToolbar()
        }

        (activity as? VideoPlayerActivity)?.setDraggable(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateAddSubtitleState()
        if (serviceGateway?.playing() == true) {
            serviceGateway?.setPlayWhenReady(false)
            viewModel.updateVideoPlayerPausedForPlaylist(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
        // Close the dialog after fragment is destroyed to avoid adding dialog view repeatedly after screen is rotated.
        playbackPositionDialog?.run {
            if (isShowing) dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceGateway?.removeListener(playerListener)
        serviceGateway = null
        context?.unbindService(connection)
    }

    private fun observeFlow() {
        if (view != null) {
            serviceGateway?.let { gateway ->
                collectFlow(gateway.metadataUpdate()) { metadata ->
                    playerViewHolder?.displayMetadata(metadata)
                }

                collectFlow(gateway.playbackPositionStateUpdate()) { state ->
                    if (state.showPlaybackDialog) {
                        playbackPositionDialog =
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.video_playback_position_dialog_title)
                                .setMessage(
                                    String.format(
                                        getString(
                                            R.string.video_playback_position_dialog_message
                                        ),
                                        state.mediaItemName,
                                        viewModel.formatMillisecondsToString(
                                            state.playbackPosition ?: 0
                                        )
                                    )
                                )
                                .setNegativeButton(
                                    R.string.video_playback_position_dialog_resume_button
                                ) { _, _ ->
                                    if (state.isDialogShownBeforeBuildSources)
                                        gateway.setResumePlaybackPositionBeforeBuildSources(
                                            state.playbackPosition
                                        )
                                    else
                                        gateway.setResumePlaybackPosition(state.playbackPosition)

                                }
                                .setPositiveButton(
                                    R.string.video_playback_position_dialog_restart_button
                                ) { _, _ ->
                                    if (state.isDialogShownBeforeBuildSources)
                                        gateway.setRestartPlayVideoBeforeBuildSources()
                                    else
                                        gateway.setRestartPlayVideo()
                                }
                                .setOnCancelListener {
                                    if (state.isDialogShownBeforeBuildSources) {
                                        gateway.cancelPlaybackPositionDialogBeforeBuildSources()
                                    } else {
                                        gateway.cancelPlaybackPositionDialog()
                                    }
                                }.create().apply {
                                    show()
                                }
                    }
                }
            }

            serviceViewModelGateway?.let {
                if (!playlistObserved) {
                    playlistObserved = true
                    collectFlow(it.playlistUpdate()) { info ->
                        Timber.d("MediaPlayerService observed playlist ${info.first.size} items")
                        playerViewHolder?.togglePlaylistEnabled(info.first)
                    }

                    collectFlow(it.retryUpdate()) { isRetry ->
                        when {
                            !isRetry && retryFailedDialog == null -> {
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
                                            serviceGateway?.stopAudioPlayer()
                                            requireActivity().finish()
                                        }
                                        .show()
                            }

                            isRetry -> {
                                retryFailedDialog?.dismiss()
                                retryFailedDialog = null
                            }
                        }
                    }

                    collectFlow(it.mediaPlaybackUpdate()) { isPaused ->
                        // The keepScreenOn is true when the video is playing, otherwise it's false.
                        videoPlayerView?.keepScreenOn = !isPaused
                    }
                }
            }

            collectFlow(viewModel.state) { state ->
                playerViewHolder?.updateSubtitleButtonUI(state.isSubtitleShown)
                serviceGateway?.setPlayWhenReady(!state.isSubtitleDialogShown)
                if (!state.isSubtitleDialogShown) {
                    delayHideToolbar()
                }
                if (state.isSubtitleShown) {
                    if (state.isAddSubtitle) {
                        state.subtitleFileInfo?.url?.let {
                            serviceGateway?.addSubtitle(it)
                        }
                    }
                    serviceGateway?.showSubtitle()
                } else {
                    serviceGateway?.hideSubtitle()
                }
            }
        }
    }

    private fun setupPlayer() {
        playerViewHolder?.let { viewHolder ->
            videoPlayerView = viewHolder.binding.playerView
            with(viewHolder) {
                // we need setup control buttons again, because reset player would reset PlayerControlView
                serviceViewModelGateway?.run {
                    setupPlaylistButton(getPlaylistItems()) {
                        (activity as? VideoPlayerActivity)?.setDraggable(false)
                        findNavController().navigate(R.id.action_player_to_playlist)
                    }
                }

                setupLockUI(viewModel.isLockUpdate.value) { isLock ->
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
                        isSubtitleShown = viewModel.state.value.isSubtitleShown
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
                            captureAreaView = requireActivity().window.decorView,
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
                                (activity as? VideoPlayerActivity)?.showSnackbarForVideoPlayer(
                                    getString(R.string.media_player_video_snackbar_screenshot_saved)
                                )
                            }
                        }
                    }
                }
            }

            serviceGateway?.run {
                setupPlayerView(this, viewHolder.binding.playerView)

                if (viewModel.state.value.videoPlayerPausedForPlaylistState) {
                    setPlayWhenReady(true)
                    viewModel.updateVideoPlayerPausedForPlaylist(false)
                }
            }

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
        view.setImageBitmap(bitmap)
        val scaleY =
            if (requireActivity().configuration.orientation == ORIENTATION_LANDSCAPE) {
                SCREENSHOT_SCALE_LANDSCAPE
            } else {
                SCREENSHOT_SCALE_PORTRAIT
            }
        val anim: Animation = ScaleAnimation(
            SCREENSHOT_SCALE_ORIGINAL, scaleY,  // Start and end values for the X axis scaling
            SCREENSHOT_SCALE_ORIGINAL, scaleY,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, SCREENSHOT_RELATE_X,  // Pivot point of X scaling
            Animation.RELATIVE_TO_PARENT, SCREENSHOT_RELATE_Y
        ) // Pivot point of Y scaling

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                view.postDelayed({
                    view.setImageDrawable(null)
                    layout.isVisible = false
                    layout.clearAnimation()
                    bitmap.recycle()
                }, SCREENSHOT_DURATION)
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })
        anim.fillAfter = true
        anim.duration = ANIMATION_DURATION
        layout.startAnimation(anim)
    }

    private fun initRepeatToggleButtonForVideo(viewHolder: VideoPlayerViewHolder) {
        serviceGateway?.run {
            val defaultRepeatMode = when (
                requireContext().defaultSharedPreferences.getInt(
                    KEY_VIDEO_REPEAT_MODE, RepeatToggleMode.REPEAT_NONE.ordinal
                )
            ) {
                RepeatToggleMode.REPEAT_NONE.ordinal -> RepeatToggleMode.REPEAT_NONE
                RepeatToggleMode.REPEAT_ONE.ordinal -> RepeatToggleMode.REPEAT_ONE
                else -> RepeatToggleMode.REPEAT_ALL
            }

            setRepeatModeForVideo(
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
                val repeatToggleMode =
                    serviceViewModelGateway?.videoRepeatToggleMode() ?: RepeatToggleMode.REPEAT_NONE

                if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                    setRepeatModeForVideo(RepeatToggleMode.REPEAT_ONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.teal_300))
                    viewModel.sendLoopButtonEnabledEvent()
                } else {
                    setRepeatModeForVideo(RepeatToggleMode.REPEAT_NONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.white))
                }
            }
        }
    }

    private fun setupPlayerView(
        mediaPlayerServiceGateway: MediaPlayerServiceGateway,
        playerView: StyledPlayerView,
    ) {
        mediaPlayerServiceGateway.setupPlayerView(
            playerView = playerView,
            isAudioPlayer = false,
            controllerHideOnTouch = true,
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
                if (viewModel.isLockUpdate.value) {
                    delayHideToolbar()
                }
            }
        }
        updateLoadingAnimation(mediaPlayerServiceGateway.getPlaybackState())
        mediaPlayerServiceGateway.addPlayerListener(playerListener)
    }

    private fun updateLoadingAnimation(@Player.State playbackState: Int) =
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
            if (viewModel.isLockUpdate.value) {
                hideToolbar()
                playerViewHolder?.hideController()
            }
        }
    }

    private fun hideToolbar(animate: Boolean = true) {
        toolbarVisible = false
        (activity as? VideoPlayerActivity)?.hideToolbar(animate)
    }

    private fun showToolbar() {
        toolbarVisible = true
        val activity = activity as? VideoPlayerActivity
        if (viewModel.isLockUpdate.value) {
            activity?.showSystemUI()
        } else {
            activity?.showToolbar()
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
                if (viewModel.state.collectAsState().value.isSubtitleDialogShown) {
                    AddSubtitleDialog(
                        selectOptionState = viewModel.selectOptionState,
                        matchedSubtitleFileUpdate = {
                            serviceViewModelGateway?.getMatchedSubtitleFileInfoForPlayingItem()
                        },
                        subtitleFileName = viewModel.subtitleInfoByAddSubtitles?.name,
                        onOffClicked = {
                            viewModel.onOffItemClicked()
                        },
                        onAddedSubtitleClicked = {
                            viewModel.onAddedSubtitleOptionClicked()
                        },
                        onAutoMatch = { info ->
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
                                        viewModel.state.value.subtitleFileInfo?.id
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

    /**
     * Show the subtitle icon when the adapterType is not OFFLINE_ADAPTER
     */
    private fun showSubtitleIcon() =
        activity?.intent?.getIntExtra(
            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
            Constants.INVALID_VALUE
        ) != Constants.OFFLINE_ADAPTER

    companion object {
        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "MEGA Screenshots"
        private const val SCREENSHOT_SCALE_ORIGINAL: Float = 1F
        private const val SCREENSHOT_SCALE_PORTRAIT: Float = 0.4F
        private const val SCREENSHOT_SCALE_LANDSCAPE: Float = 0.3F
        private const val SCREENSHOT_RELATE_X: Float = 0.9F
        private const val SCREENSHOT_RELATE_Y: Float = 0.6F
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