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
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.mediaplayer.service.*
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import java.util.*

class MediaPlayerFragment : Fragment() {
    private var audioPlayerVH: AudioPlayerViewHolder? = null
    private var videoPlayerVH: VideoPlayerViewHolder? = null

    private var playerService: MediaPlayerService? = null

    private var playlistObserved = false
    private var videoPlayerPausedForPlaylist = false

    private var delayHideToolbarCanceled = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                setupPlayer(service.service)
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
        savedInstanceState: Bundle?
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

        if (savedInstanceState != null) {
            videoPlayerPausedForPlaylist =
                savedInstanceState.getBoolean(KEY_VIDEO_PAUSED_FOR_PLAYLIST, false)
        }

        val isAudioPlayer = MediaPlayerActivity.isAudioPlayer(requireActivity().intent)
        val playerServiceIntent = Intent(
            requireContext(),
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()

        val service = playerService
        if (service != null) {
            setupPlayer(service)
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

        if (isVideoPlayer() && playerService?.playing() == true) {
            playerService?.setPlayWhenReady(false)
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

        playerService?.player?.removeListener(playerListener)
        playerService = null
        requireContext().unbindService(connection)
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        val service = playerService
        if (!playlistObserved && service != null && view != null) {
            playlistObserved = true

            service.viewModel.playlist.observe(viewLifecycleOwner) {
                logDebug("MediaPlayerService observed playlist ${it.first.size} items")

                audioPlayerVH?.togglePlaylistEnabled(it.first)
                videoPlayerVH?.togglePlaylistEnabled(it.first)
            }

            service.viewModel.retry.observe(viewLifecycleOwner) {
                when {
                    !it && retryFailedDialog == null -> {
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
                                    .uppercase(Locale.ROOT)
                            ) { _, _ ->
                                playerService?.stopAudioPlayer()
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
    }

    private fun setupPlayer(service: MediaPlayerService) {
        if (MediaPlayerActivity.isAudioPlayer(activity?.intent)) {
            val viewHolder = audioPlayerVH ?: return

            setupPlayerView(service.player, viewHolder.binding.playerView, false)
            viewHolder.layoutArtwork()
            service.metadata.observe(viewLifecycleOwner, viewHolder::displayMetadata)

            viewHolder.setupPlaylistButton(service.viewModel.playlist.value?.first) {
                findNavController().navigate(R.id.action_player_to_playlist)
            }
        } else {
            val viewHolder = videoPlayerVH ?: return

            setupPlayerView(service.player, viewHolder.binding.playerView, true)
            service.metadata.observe(viewLifecycleOwner, viewHolder::displayMetadata)

            // we need setup control buttons again, because reset player would reset
            // PlayerControlView
            viewHolder.setupPlaylistButton(service.viewModel.playlist.value?.first) {
                (requireActivity() as MediaPlayerActivity).setDraggable(false)

                findNavController().navigate(R.id.action_player_to_playlist)
            }

            if (videoPlayerPausedForPlaylist) {
                service.setPlayWhenReady(true)
                videoPlayerPausedForPlaylist = false
            }
        }
    }

    private fun setupPlayerView(
        player: MediaMegaPlayer,
        playerView: PlayerView,
        videoPlayer: Boolean
    ) {
        with(playerView) {
            this.player = player

            useController = true
            controllerShowTimeoutMs = 0

            setRepeatToggleModes(
                if (videoPlayer)
                    RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
                else
                    RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
            )

            controllerHideOnTouch = videoPlayer
            setShowShuffleButton(!videoPlayer)
            setControllerVisibilityListener { visibility ->
                if (visibility == View.VISIBLE && !toolbarVisible) {
                    hideController()
                }
            }

            setOnClickListener {
                if (toolbarVisible) {
                    hideToolbar()
                } else {
                    delayHideToolbarCanceled = true

                    showToolbar()
                }
            }

            showController()
        }

        updateLoadingAnimation(player.playbackState)
        player.wrappedPlayer.addListener(playerListener)
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

    private fun isVideoPlayer() = playerService?.viewModel?.audioPlayer == false

    companion object {
        private const val KEY_VIDEO_PAUSED_FOR_PLAYLIST = "VIDEO_PAUSED_FOR_PLAYLIST"
    }
}
