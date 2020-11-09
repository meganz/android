package mega.privacy.android.app.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.utils.autoCleared

@AndroidEntryPoint
class AudioPlayerFragment : Fragment() {
    private var binding by autoCleared<FragmentAudioPlayerBinding>()

    private lateinit var playerServiceIntent: Intent
    private var playerService: AudioPlayerService? = null

    private var bgPlayEnabled = true

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioPlayerServiceBinder) {
                playerService = service.service

                setupPlayer(service.service)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back_white)!!.mutate()
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = activity?.intent?.extras ?: return
        playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        playerServiceIntent.putExtras(extras)
        playerServiceIntent.setDataAndType(activity?.intent?.data, activity?.intent?.type)
        Util.startForegroundService(requireContext(), playerServiceIntent)

        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()

        val service = playerService
        if (service != null) {
            setupPlayer(service)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService?.mainPlayerUIClosed()
        playerService = null
        requireContext().unbindService(connection)
    }

    private fun setupPlayer(service: AudioPlayerService) {
        setupPlayerView(service.exoPlayer)
        observeMetadata(service.metadata)
        listenButtons()
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

        binding.playerView.setControlDispatcher(AudioPlayerControlDispatcher())

        binding.playerView.showController()
    }

    private fun observeMetadata(metadata: LiveData<Metadata>) {
        metadata.observe(viewLifecycleOwner, this::displayMetadata)
    }

    private fun displayMetadata(metadata: Metadata) {
        val trackName = binding.root.findViewById<TextView>(R.id.track_name)
        val artistName = binding.root.findViewById<TextView>(R.id.artist_name)
        if (metadata.title != null && metadata.artist != null) {
            setTrackNameBottomMargin(trackName, true)
            trackName.text = metadata.title

            artistName.isVisible = true
            artistName.text = metadata.artist
        } else {
            setTrackNameBottomMargin(trackName, false)
            trackName.text = metadata.nodeName

            artistName.isVisible = false
        }
    }

    private fun setTrackNameBottomMargin(trackName: TextView, small: Boolean) {
        val params = trackName.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(
            if (small) R.dimen.audio_player_music_name_margin_bottom_small
            else R.dimen.audio_player_music_name_margin_bottom_large
        )
        trackName.layoutParams = params
    }

    private fun listenButtons() {
        listenBgPlaySetting()
        listenPlaylistButton()
    }

    private fun listenBgPlaySetting() {
        val bgPlay = binding.root.findViewById<ImageButton>(R.id.background_play_toggle)
        updateBgPlay(bgPlay)
        bgPlay.setOnClickListener {
            bgPlayEnabled = !bgPlayEnabled
            updateBgPlay(bgPlay)
        }
    }

    private fun updateBgPlay(bgPlay: ImageButton) {
        bgPlay.setImageResource(
            if (bgPlayEnabled) R.drawable.player_play_bg_on else R.drawable.player_play_bg_off
        )
        playerService?.toggleBackgroundPlay(bgPlayEnabled)
    }

    private fun listenPlaylistButton() {
        binding.root.findViewById<ImageButton>(R.id.playlist).setOnClickListener {
            findNavController().navigate(R.id.action_player_to_playlist)
        }
    }
}
