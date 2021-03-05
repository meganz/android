package mega.privacy.android.app.mediaplayer.playlist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.RepeatModeUtil
import mega.privacy.android.app.databinding.FragmentAudioPlaylistBinding
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.service.*
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.autoCleared

class PlaylistFragment : Fragment(), PlaylistItemOperation {
    private var binding by autoCleared<FragmentAudioPlaylistBinding>()

    private var playerService: MediaPlayerService? = null

    private lateinit var adapter: PlaylistAdapter
    private lateinit var listLayoutManager: LinearLayoutManager

    private var playlistObserved = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                if (service.service.viewModel.audioPlayer) {
                    setupPlayerView(service.service.exoPlayer)
                } else {
                    binding.playerView.isVisible = false

                    val layoutParams = binding.playlist.layoutParams as FrameLayout.LayoutParams
                    layoutParams.bottomMargin = 0
                    binding.playlist.layoutParams = layoutParams
                }

                tryObservePlaylist()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MediaPlayerActivity).showToolbar(false)

        adapter = PlaylistAdapter(this, (requireActivity() as MediaPlayerActivity).isAudioPlayer())

        binding.playlist.setHasFixedSize(true)
        listLayoutManager = LinearLayoutManager(requireContext())
        binding.playlist.layoutManager = listLayoutManager
        binding.playlist.adapter = adapter

        binding.playlist.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                (requireActivity() as MediaPlayerActivity).showToolbarElevation(
                    recyclerView.canScrollVertically(-1)
                )
            }
        })

        tryObservePlaylist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isAudioPlayer = MediaPlayerActivity.isAudioPlayer(requireActivity().intent)
        val playerServiceIntent = Intent(
            requireContext(),
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

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
                adapter.submitList(it.first) {
                    listLayoutManager.scrollToPositionWithOffset(it.second, 0)
                }

                (requireActivity() as MediaPlayerActivity).setToolbarTitle(it.third)
            }
        }
    }

    private fun setupPlayerView(player: SimpleExoPlayer) {
        binding.playerView.player = player

        binding.playerView.useController = true
        binding.playerView.controllerShowTimeoutMs = 0
        binding.playerView.controllerHideOnTouch = false
        binding.playerView.setShowShuffleButton(true)
        binding.playerView.setRepeatToggleModes(
            RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
        )

        binding.playerView.showController()

        binding.playerView.setControlDispatcher(CallAwareControlDispatcher(player.repeatMode))
    }

    override fun onItemClick(item: PlaylistItem) {
        if (!CallUtil.participatingInACall()) {
            playerService?.exoPlayer?.seekTo(item.index, 0)
        }
        (requireActivity() as MediaPlayerActivity).closeSearch()

        if (isVideoPlayer()) {
            (requireActivity() as MediaPlayerActivity).onBackPressed()
        }
    }

    override fun removeItem(item: PlaylistItem) {
        playerService?.viewModel?.removeItem(item.nodeHandle)
    }

    private fun isVideoPlayer() = playerService?.viewModel?.audioPlayer == false
}
