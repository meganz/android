package mega.privacy.android.app.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.constants.SettingsConstants.KEY_VIDEO_REPEAT_MODE
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.ViewUtils.isVisible
import org.jetbrains.anko.configuration
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.io.File

/**
 * MediaPlayer Fragment
 */
class MediaPlayerFragment : Fragment() {
    private var audioPlayerVH: AudioPlayerViewHolder? = null
    private var videoPlayerVH: VideoPlayerViewHolder? = null

    private val viewModel: MediaPlayerViewModel by activityViewModels()

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var playerServiceViewModelGateway: PlayerServiceViewModelGateway? = null

    private var playlistObserved = false
    private var videoPlayerPausedForPlaylist = false

    private var delayHideToolbarCanceled = false

    private var videoPlayerView: PlayerView? = null

    private var isAudioPlayer = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
            playerServiceViewModelGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                playerServiceViewModelGateway = service.playerServiceViewModelGateway

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
        }
    }

    private var toolbarVisible = true

    private var retryFailedDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        if (MediaPlayerActivity.isAudioPlayer(activity?.intent)) {
            val binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
            audioPlayerVH = AudioPlayerViewHolder(binding)
            binding.root
        } else {
            val binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
            videoPlayerVH = VideoPlayerViewHolder(binding)
            binding.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFlow()

        if (isAudioPlayer) {
            delayHideToolbar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAudioPlayer = MediaPlayerActivity.isAudioPlayer(requireActivity().intent)
        if (savedInstanceState != null) {
            videoPlayerPausedForPlaylist =
                savedInstanceState.getBoolean(KEY_VIDEO_PAUSED_FOR_PLAYLIST, false)
        }
        val playerServiceIntent = Intent(
            requireContext(),
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()

        if (serviceGateway != null && playerServiceViewModelGateway != null) {
            setupPlayer()
        }

        if (!toolbarVisible) {
            showToolbar()
            delayHideToolbar()
        }

        if (isVideoPlayer()) {
            (requireActivity() as MediaPlayerActivity).setDraggable(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isVideoPlayer() && serviceGateway?.playing() == true) {
            serviceGateway?.setPlayWhenReady(false)
            videoPlayerPausedForPlaylist = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VIDEO_PAUSED_FOR_PLAYLIST, videoPlayerPausedForPlaylist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceGateway?.removeListener(playerListener)
        serviceGateway = null
        requireContext().unbindService(connection)
    }

    private fun observeFlow() {
        if (view != null) {
            serviceGateway?.run {
                metadataUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { metadata ->
                    if (isAudioPlayer) {
                        audioPlayerVH?.displayMetadata(metadata)
                    } else {
                        videoPlayerVH?.displayMetadata(metadata)
                    }
                }.launchIn(viewLifecycleOwner.lifecycleScope)
            }
            playerServiceViewModelGateway?.run {
                if (!playlistObserved) {
                    playlistObserved = true
                    playlistUpdate().flowWithLifecycle(
                        viewLifecycleOwner.lifecycle,
                        Lifecycle.State.RESUMED
                    ).onEach {
                        Timber.d("MediaPlayerService observed playlist ${it.first.size} items")

                        audioPlayerVH?.togglePlaylistEnabled(requireContext(), it.first)
                        videoPlayerVH?.togglePlaylistEnabled(it.first)
                    }.launchIn(viewLifecycleOwner.lifecycleScope)

                    retryUpdate().flowWithLifecycle(
                        viewLifecycleOwner.lifecycle,
                        Lifecycle.State.RESUMED
                    ).onEach { isRetry ->
                        when {
                            !isRetry && retryFailedDialog == null -> {
                                retryFailedDialog = MaterialAlertDialogBuilder(requireContext())
                                    .setCancelable(false)
                                    .setMessage(
                                        StringResourcesUtils.getString(
                                            if (isOnline(requireContext())) R.string.error_fail_to_open_file_general
                                            else R.string.error_fail_to_open_file_no_network
                                        )
                                    )
                                    .setPositiveButton(
                                        StringResourcesUtils.getString(R.string.general_ok)
                                    ) { _, _ ->
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
                    }.launchIn(viewLifecycleOwner.lifecycleScope)

                    mediaPlaybackUpdate().flowWithLifecycle(
                        viewLifecycleOwner.lifecycle,
                        Lifecycle.State.RESUMED
                    ).onEach { isPaused ->
                        if (isVideoPlayer()) {
                            // The keepScreenOn is true when the video is playing, otherwise it's false.
                            videoPlayerView?.keepScreenOn = !isPaused
                        }
                    }.launchIn(viewLifecycleOwner.lifecycleScope)
                }
            }
        }
    }

    private fun setupPlayer() {
        if (isAudioPlayer) {
            val viewHolder = audioPlayerVH ?: return

            serviceGateway?.run {
                setupPlayerView(this, viewHolder.binding.playerView, false)
                viewHolder.layoutArtwork()
            }

            playerServiceViewModelGateway?.run {
                viewHolder.setupPlaylistButton(requireContext(), getPlaylistItems()) {
                    findNavController().navigate(R.id.action_player_to_playlist)
                }
            }
        } else {
            videoPlayerVH?.let { viewHolder ->
                videoPlayerView = viewHolder.binding.playerView
                serviceGateway?.run {
                    setupPlayerView(this, viewHolder.binding.playerView, true)

                    if (videoPlayerPausedForPlaylist) {
                        setPlayWhenReady(true)
                        videoPlayerPausedForPlaylist = false
                    }
                }
                with(viewHolder) {
                    // we need setup control buttons again, because reset player would reset PlayerControlView
                    setupPlaylistButton(playerServiceViewModelGateway?.getPlaylistItems()) {
                        (requireActivity() as MediaPlayerActivity).setDraggable(false)
                        findNavController().navigate(R.id.action_player_to_playlist)
                    }
                    setupLockUI(viewModel.isLockUpdate.value) { isLock ->
                        viewModel.updateLockStatus(isLock)
                        if (isLock) {
                            delayHideWhenLocked()
                        }
                    }
                    setupScreenshotButton {
                        val rootPath =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
                        val screenshotsFolderPath =
                            "${rootPath}${File.separator}$MEGA_SCREENSHOTS_FOLDER_NAME${File.separator}"
                        viewHolder.binding.playerView.videoSurfaceView?.let { view ->
                            viewModel.screenshotWhenVideoPlaying(requireActivity().window.decorView,
                                screenshotsFolderPath,
                                view) { bitmap ->
                                requireActivity().runOnUiThread {
                                    showCaptureScreenshotAnimation(
                                        view = binding.screenshotScaleAnimationView,
                                        layout = binding.screenshotScaleAnimationLayout,
                                        bitmap = bitmap
                                    )
                                    (requireActivity() as MediaPlayerActivity)
                                        .showSnackbarForVideoPlayer(
                                            getString(R.string.media_player_video_snackbar_screenshot_saved)
                                        )
                                }
                            }
                        }
                    }
                }
                initRepeatToggleButtonForVideo(viewHolder)
            }
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
        val scaleY = if (requireActivity().configuration.orientation == ORIENTATION_LANDSCAPE) {
            SCREENSHOT_SCALE_LANDSCAPE
        } else {
            SCREENSHOT_SCALE_PORTRAIT
        }
        val anim: Animation = ScaleAnimation(
            SCREENSHOT_SCALE_ORIGINAL, scaleY,  // Start and end values for the X axis scaling
            SCREENSHOT_SCALE_ORIGINAL, scaleY,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, SCREENSHOT_RELATE_X,  // Pivot point of X scaling
            Animation.RELATIVE_TO_PARENT, SCREENSHOT_RELATE_Y) // Pivot point of Y scaling

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

            override fun onAnimationRepeat(p0: Animation?) {
            }

        })
        anim.fillAfter = true
        anim.duration = ANIMATION_DURATION
        layout.startAnimation(anim)
    }

    private fun initRepeatToggleButtonForVideo(viewHolder: VideoPlayerViewHolder) {
        serviceGateway?.run {
            val defaultRepeatMode = when (requireContext().defaultSharedPreferences.getInt(
                KEY_VIDEO_REPEAT_MODE, RepeatToggleMode.REPEAT_NONE.ordinal)) {
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
            viewHolder.setupRepeatToggleButton(requireContext(),
                defaultRepeatMode) { repeatToggleButton ->
                val repeatToggleMode =
                    playerServiceViewModelGateway?.videoRepeatToggleMode()
                        ?: RepeatToggleMode.REPEAT_NONE

                if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                    setRepeatModeForVideo(RepeatToggleMode.REPEAT_ONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.teal_300))
                } else {
                    setRepeatModeForVideo(RepeatToggleMode.REPEAT_NONE)
                    repeatToggleButton.setColorFilter(requireContext().getColor(R.color.white))
                }
            }
        }
    }

    private fun setupPlayerView(
        mediaPlayerServiceGateway: MediaPlayerServiceGateway,
        playerView: PlayerView,
        isVideoPlayer: Boolean,
    ) {
        mediaPlayerServiceGateway.setupPlayerView(
            playerView = playerView,
            isAudioPlayer = isAudioPlayer,
            controllerHideOnTouch = isVideoPlayer,
            showShuffleButton = !isVideoPlayer,
        )
        playerView.setControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE && !toolbarVisible) {
                playerView.hideController()
            }
        }
        playerView.setOnClickListener {
            if (toolbarVisible) {
                hideToolbar()
            } else {
                delayHideToolbarCanceled = true
                showToolbar()
            }
        }
        updateLoadingAnimation(mediaPlayerServiceGateway.getPlaybackState())
        mediaPlayerServiceGateway.addPlayerListener(playerListener)
    }

    private fun updateLoadingAnimation(@Player.State playbackState: Int) {
        audioPlayerVH?.updateLoadingAnimation(playbackState)
        videoPlayerVH?.updateLoadingAnimation(playbackState)
    }

    private fun delayHideToolbar() {
        delayHideToolbarCanceled = false

        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed && !delayHideToolbarCanceled) {
                hideToolbar()

                videoPlayerVH?.hideController()
            }
        }
    }

    private fun delayHideWhenLocked() {
        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (viewModel.isLockUpdate.value) {
                hideToolbar()
                videoPlayerVH?.hideController()
            }
        }
    }

    private fun hideToolbar(animate: Boolean = true) {
        toolbarVisible = false
        (requireActivity() as MediaPlayerActivity).hideToolbar(animate)
    }

    private fun showToolbar() {
        toolbarVisible = true
        val mediaPlayerActivity = requireActivity() as MediaPlayerActivity
        if (isAudioPlayer) {
            mediaPlayerActivity.showToolbar()
        } else {
            if (viewModel.isLockUpdate.value) {
                mediaPlayerActivity.showSystemUI()
            } else {
                mediaPlayerActivity.showToolbar()
            }
        }
    }

    /**
     * Run the enter animation
     *
     * @param dragToExit DragToExitSupport
     */
    fun runEnterAnimation(dragToExit: DragToExitSupport) {
        val binding = videoPlayerVH?.binding ?: return

        dragToExit.runEnterAnimation(requireActivity().intent, binding.playerView) {
            if (it) {
                updateViewForAnimation()
            } else if (isResumed) {
                showToolbar()
                videoPlayerVH?.showController()

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

            val videoSurfaceView = videoPlayerVH?.binding?.playerView?.videoSurfaceView ?: return
            dragToExit.setCurrentView(videoSurfaceView)
        } else {
            runDelay(300L) {
                videoPlayerVH?.binding?.root?.setBackgroundColor(Color.BLACK)
            }
        }
    }

    private fun updateViewForAnimation() {
        hideToolbar(animate = false)
        videoPlayerVH?.hideController()

        videoPlayerVH?.binding?.root?.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun isVideoPlayer() = playerServiceViewModelGateway?.isAudioPlayer() == false

    companion object {
        private const val KEY_VIDEO_PAUSED_FOR_PLAYLIST = "VIDEO_PAUSED_FOR_PLAYLIST"
        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "MEGA Screenshots"
        private const val SCREENSHOT_SCALE_ORIGINAL: Float = 1F
        private const val SCREENSHOT_SCALE_PORTRAIT: Float = 0.4F
        private const val SCREENSHOT_SCALE_LANDSCAPE: Float = 0.3F
        private const val SCREENSHOT_RELATE_X: Float = 0.9F
        private const val SCREENSHOT_RELATE_Y: Float = 0.6F
        private const val ANIMATION_DURATION: Long = 500
        private const val SCREENSHOT_DURATION: Long = 500
    }
}
