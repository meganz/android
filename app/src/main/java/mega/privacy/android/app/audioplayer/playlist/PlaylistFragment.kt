package mega.privacy.android.app.audioplayer.playlist

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.RepeatModeUtil
import mega.privacy.android.app.audioplayer.AudioPlayerActivity
import mega.privacy.android.app.audioplayer.AudioPlayerService
import mega.privacy.android.app.audioplayer.AudioPlayerServiceBinder
import mega.privacy.android.app.databinding.FragmentAudioPlaylistBinding
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.autoCleared

class PlaylistFragment : Fragment(), PlaylistItemOperation {
    private var binding by autoCleared<FragmentAudioPlaylistBinding>()

    private var playerService: AudioPlayerService? = null

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
            if (service is AudioPlayerServiceBinder) {
                playerService = service.service

                setupPlayerView(service.service.exoPlayer)
                tryObservePlaylist()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAudioPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AudioPlayerActivity).showToolbar(false)

        adapter = PlaylistAdapter(this)

        binding.playlist.setHasFixedSize(true)
        listLayoutManager = LinearLayoutManager(requireContext())
        binding.playlist.layoutManager = listLayoutManager
        binding.playlist.adapter = adapter

        tryObservePlaylist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService = null
        requireContext().unbindService(connection)
    }

    private fun tryObservePlaylist() {
        val service = playerService
        if (!playlistObserved && service != null) {
            playlistObserved = true

            service.viewModel.playlist.observe(viewLifecycleOwner) {
                adapter.submitList(it.first) {
                    listLayoutManager.scrollToPositionWithOffset(it.second, 0)
                }

                (requireActivity() as AudioPlayerActivity).setToolbarTitle(it.third)
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
    }

    override fun onItemClick(item: PlaylistItem) {
        playerService?.exoPlayer?.seekTo(item.index, 0)
        (requireActivity() as AudioPlayerActivity).closeSearch()
    }

    override fun openItemOptionPanel(item: PlaylistItem) {
    }
}
