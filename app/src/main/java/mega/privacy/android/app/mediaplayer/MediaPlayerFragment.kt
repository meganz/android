package mega.privacy.android.app.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import timber.log.Timber

/**
 * MediaPlayer Fragment
 */
class MediaPlayerFragment : Fragment() {
    private var audioPlayerVH: AudioPlayerViewHolder? = null
    private var videoPlayerVH: VideoPlayerViewHolder? = null

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
                tryObservePlaylist()
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

        tryObservePlaylist()

        if (!isVideoPlayer()) {
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

        setupPlayer()

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

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        playerServiceViewModelGateway?.run {
            if (!playlistObserved && view != null) {
                playlistObserved = true

                playlistUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach {
                    Timber.d("MediaPlayerService observed playlist ${it.first.size} items")

                    audioPlayerVH?.togglePlaylistEnabled(it.first)
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

    private fun setupPlayer() {
        if (isAudioPlayer) {
            val viewHolder = audioPlayerVH ?: return

            serviceGateway?.run {
                setupPlayerView(this, viewHolder.binding.playerView, false)
                viewHolder.layoutArtwork()
                metadataUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { metadata ->
                    viewHolder.displayMetadata(metadata)
                }.launchIn(viewLifecycleOwner.lifecycleScope)
            }

            playerServiceViewModelGateway?.run {
                viewHolder.setupPlaylistButton(getPlaylistItems()) {
                    findNavController().navigate(R.id.action_player_to_playlist)
                }
            }
        } else {
            val viewHolder = videoPlayerVH ?: return
            videoPlayerView = viewHolder.binding.playerView

            serviceGateway?.run {
                setupPlayerView(this, viewHolder.binding.playerView, true)
                metadataUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { metadata ->
                    viewHolder.displayMetadata(metadata)
                }.launchIn(viewLifecycleOwner.lifecycleScope)

                if (videoPlayerPausedForPlaylist) {
                    setPlayWhenReady(true)
                    videoPlayerPausedForPlaylist = false
                }
            }
            playerServiceViewModelGateway?.run {
                // we need setup control buttons again, because reset player would reset PlayerControlView
                viewHolder.setupPlaylistButton(getPlaylistItems()) {
                    (requireActivity() as MediaPlayerActivity).setDraggable(false)
                    findNavController().navigate(R.id.action_player_to_playlist)
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
            repeatToggleModes = if (isVideoPlayer) {
                REPEAT_TOGGLE_MODE_NONE
            } else {
                REPEAT_TOGGLE_MODE_ONE or REPEAT_TOGGLE_MODE_ALL
            },
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

    private fun hideToolbar(animate: Boolean = true) {
        toolbarVisible = false

        (requireActivity() as MediaPlayerActivity).hideToolbar(animate)
    }

    private fun showToolbar() {
        toolbarVisible = true

        (requireActivity() as MediaPlayerActivity).showToolbar()
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
    }
}
