package mega.privacy.android.app.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.CallAwareControlDispatcher
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.Util.isOnline
import java.util.*

class MediaPlayerFragment : Fragment() {
    private var audioPlayerVH: AudioPlayerViewHolder? = null
    private var videoPlayerVH: VideoPlayerViewHolder? = null

    private var playerService: MediaPlayerService? = null

    private var playlistObserved = false
    private var videoPlayerPausedForPlaylist = false

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
    private val playerListener = object : Player.EventListener {
        override fun onPlaybackStateChanged(state: Int) {
            if (isResumed) {
                updateLoadingAnimation(state)
            }
        }
    }

    private var toolbarVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (MediaPlayerActivity.isAudioPlayer(activity?.intent)) {
            val binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
            audioPlayerVH = AudioPlayerViewHolder(binding)
            binding.root
        } else {
            val binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
            videoPlayerVH = VideoPlayerViewHolder(binding)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tryObservePlaylist()
        delayHideToolbar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerServiceIntent = Intent(requireContext(), MediaPlayerService::class.java)
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

        if (videoPlayerPausedForPlaylist) {
            playerService?.exoPlayer?.playWhenReady = true
            videoPlayerPausedForPlaylist = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService?.exoPlayer?.removeListener(playerListener)
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
                audioPlayerVH?.togglePlaylistEnabled(it.first)
                videoPlayerVH?.togglePlaylistEnabled(it.first)
            }

            service.viewModel.retry.observe(viewLifecycleOwner) {
                if (!it) {
                    MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialogStyle)
                        .setCancelable(false)
                        .setMessage(
                            if (isOnline(requireContext())) R.string.error_fail_to_open_file_general
                            else R.string.error_fail_to_open_file_no_network
                        )
                        .setPositiveButton(
                            resources.getString(R.string.general_ok).toUpperCase(Locale.ROOT)
                        ) { _, _ ->
                            playerService?.stopAudioPlayer()
                            requireActivity().finish()
                        }
                        .show()
                }
            }
        }
    }

    private fun setupPlayer(service: MediaPlayerService) {
        if (MediaPlayerActivity.isAudioPlayer(activity?.intent)) {
            val viewHolder = audioPlayerVH ?: return

            setupPlayerView(service.exoPlayer, viewHolder.binding.playerView, false)
            viewHolder.layoutArtwork()
            service.metadata.observe(viewLifecycleOwner, viewHolder::displayMetadata)

            // we need setup control buttons again, because reset player would reset
            // PlayerControlView
            viewHolder.setupBgPlaySetting(service.viewModel.backgroundPlayEnabled()) {
                playerService?.viewModel?.toggleBackgroundPlay() ?: false
            }

            viewHolder.setupPlaylistButton(service.viewModel.playlist.value?.first) {
                findNavController().navigate(R.id.action_player_to_playlist)
            }
        } else {
            val viewHolder = videoPlayerVH ?: return

            setupPlayerView(service.exoPlayer, viewHolder.binding.playerView, true)
            service.metadata.observe(viewLifecycleOwner, viewHolder::displayMetadata)

            // we need setup control buttons again, because reset player would reset
            // PlayerControlView
            viewHolder.setupPlaylistButton(service.viewModel.playlist.value?.first) {
                playerService?.exoPlayer?.playWhenReady = false
                videoPlayerPausedForPlaylist = true

                findNavController().navigate(R.id.action_player_to_playlist)
            }
        }
    }

    private fun setupPlayerView(
        player: SimpleExoPlayer,
        playerView: PlayerView,
        videoPlayer: Boolean
    ) {
        playerView.player = player

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 0

        if (videoPlayer) {
            playerView.controllerHideOnTouch = true

            playerView.setShowShuffleButton(false)
            playerView.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE)
        } else {
            playerView.controllerHideOnTouch = false

            playerView.setShowShuffleButton(true)
            playerView.setRepeatToggleModes(
                RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
            )
        }

        playerView.showController()

        playerView.setControlDispatcher(CallAwareControlDispatcher(player.repeatMode))

        playerView.setOnClickListener {
            if (toolbarVisible) {
                hideToolbar()
            } else {
                showToolbar()
            }
        }

        updateLoadingAnimation(player.playbackState)
        player.addListener(playerListener)
    }

    private fun updateLoadingAnimation(@Player.State playbackState: Int) {
        audioPlayerVH?.updateLoadingAnimation(playbackState)
        videoPlayerVH?.updateLoadingAnimation(playbackState)
    }

    private fun delayHideToolbar() {
        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed) {
                hideToolbar()

                audioPlayerVH?.hideController()
                videoPlayerVH?.hideController()
            }
        }
    }

    private fun hideToolbar() {
        toolbarVisible = false

        (requireActivity() as MediaPlayerActivity).hideToolbar()
    }

    private fun showToolbar() {
        toolbarVisible = true

        (requireActivity() as MediaPlayerActivity).showToolbar()
    }
}
