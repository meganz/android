package mega.privacy.android.app.audioplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.playlist.PlaylistItem
import mega.privacy.android.app.audioplayer.service.AudioPlayerService
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceBinder
import mega.privacy.android.app.audioplayer.service.CallAwareControlDispatcher
import mega.privacy.android.app.audioplayer.service.Metadata
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.SimpleAnimatorListener
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.autoCleared
import java.util.*

class AudioPlayerFragment : Fragment() {
    private var binding by autoCleared<FragmentAudioPlayerBinding>()

    private lateinit var artworkContainer: CardView
    private lateinit var trackName: TextView
    private lateinit var artistName: TextView
    private lateinit var bgPlay: ImageButton
    private lateinit var bgPlayHint: TextView
    private lateinit var playlist: ImageButton

    private var playerService: AudioPlayerService? = null

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

                setupPlayer(service.service)
                tryObservePlaylist()
            }
        }
    }
    private val playerListener = object : Player.EventListener {
        override fun onPlaybackStateChanged(state: Int) {
            if (isResumed) {
                binding.loading.isVisible = state == Player.STATE_BUFFERING
            }
        }
    }

    private var toolbarVisible = true

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

        artworkContainer = binding.root.findViewById(R.id.artwork_container)
        trackName = binding.root.findViewById(R.id.track_name)
        artistName = binding.root.findViewById(R.id.artist_name)
        bgPlay = binding.root.findViewById(R.id.background_play_toggle)
        bgPlayHint = binding.root.findViewById(R.id.background_play_hint)
        playlist = binding.root.findViewById(R.id.playlist)

        tryObservePlaylist()

        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed) {
                hideToolbar()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
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

        playerService?.exoPlayer?.removeListener(playerListener)
        playerService = null
        requireContext().unbindService(connection)
    }

    private fun tryObservePlaylist() {
        val service = playerService
        if (!playlistObserved && service != null) {
            playlistObserved = true

            service.viewModel.playlist.observe(viewLifecycleOwner) {
                togglePlaylistEnabled(it.first)
            }

            service.viewModel.retry.observe(viewLifecycleOwner) {
                if (!it) {
                    MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialogStyle)
                        .setCancelable(false)
                        .setMessage(
                            if (isOnline(requireContext()))
                                R.string.unsupported_file_type
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

    private fun setupPlayer(service: AudioPlayerService) {
        setupPlayerView(service.exoPlayer)
        observeMetadata(service.metadata)

        // we need setup control buttons again, because reset player would reset
        // PlayerControlView
        setupButtons()
    }

    @SuppressLint("ClickableViewAccessibility")
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

        binding.playerView.setControlDispatcher(CallAwareControlDispatcher())

        binding.playerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (toolbarVisible) {
                    hideToolbar()
                } else {
                    showToolbar()
                }
            }
            true
        }

        binding.loading.isVisible = player.playbackState == Player.STATE_BUFFERING
        player.addListener(playerListener)

        post {
            val artworkWidth = resources.displayMetrics.widthPixels / 3 * 2
            val controllerHeight =
                resources.getDimensionPixelSize(R.dimen.audio_player_main_controller_height)

            val layoutParams = artworkContainer.layoutParams as FrameLayout.LayoutParams
            layoutParams.width = artworkWidth
            layoutParams.height = artworkWidth
            layoutParams.topMargin =
                (binding.playerView.measuredHeight - artworkWidth - controllerHeight) / 2
            artworkContainer.layoutParams = layoutParams

            artworkContainer.isVisible = true
        }
    }

    private fun observeMetadata(metadata: LiveData<Metadata>) {
        metadata.observe(viewLifecycleOwner, this::displayMetadata)
    }

    private fun displayMetadata(metadata: Metadata) {
        if (metadata.title != null && metadata.artist != null) {
            if (trackName.text.isEmpty()) {
                displayTrackAndArtist(trackName, artistName, metadata)
            } else {
                animateTrackAndArtist(trackName, false) {
                    displayTrackAndArtist(trackName, artistName, metadata)
                }

                if (artistName.isVisible) {
                    animateTrackAndArtist(artistName, false)
                }
            }
        } else {
            setTrackNameBottomMargin(trackName, false)
            val needAnimate = trackName.text != metadata.nodeName
            trackName.text = metadata.nodeName
            if (needAnimate) {
                animateTrackAndArtist(trackName, true)
            }

            artistName.isVisible = false
        }
    }

    private fun displayTrackAndArtist(
        trackName: TextView,
        artistName: TextView,
        metadata: Metadata
    ) {
        setTrackNameBottomMargin(trackName, true)
        trackName.text = metadata.title
        animateTrackAndArtist(trackName, true)

        artistName.isVisible = true
        artistName.text = metadata.artist
        animateTrackAndArtist(artistName, true)
    }

    private fun animateTrackAndArtist(
        textView: TextView,
        showing: Boolean,
        listener: (() -> Unit)? = null
    ) {
        textView.alpha = if (showing) 0F else 1F

        val animator = textView.animate()
        animator.cancel()

        if (listener != null) {
            animator.setListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    animator.setListener(null)

                    listener()
                }
            })
        }

        animator
            .setDuration(AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS)
            .alpha(if (showing) 1F else 0F)
            .start()
    }

    private fun setTrackNameBottomMargin(trackName: TextView, small: Boolean) {
        val params = trackName.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(
            if (small) R.dimen.audio_player_track_name_margin_bottom_small
            else R.dimen.audio_player_track_name_margin_bottom_large
        )
        trackName.layoutParams = params
    }

    private fun setupButtons() {
        setupBgPlaySetting()
        setupPlaylistButton()
    }

    private fun setupBgPlaySetting() {
        val enabled = playerService?.viewModel?.backgroundPlayEnabled() ?: return
        updateBgPlay(bgPlay, bgPlayHint, enabled)

        bgPlay.setOnClickListener {
            val service = playerService ?: return@setOnClickListener

            service.viewModel.toggleBackgroundPlay()
            updateBgPlay(bgPlay, bgPlayHint, service.viewModel.backgroundPlayEnabled())
        }
    }

    private fun updateBgPlay(bgPlay: ImageButton, bgPlayHint: TextView, enabled: Boolean) {
        bgPlay.setImageResource(
            if (enabled) R.drawable.player_play_bg_on else R.drawable.player_play_bg_off
        )

        bgPlayHint.setText(
            if (enabled) R.string.background_play_hint else R.string.not_background_play_hint
        )
        bgPlayHint.alpha = 1F

        bgPlayHint.animate()
            .setDuration(AUDIO_PLAYER_BACKGROUND_PLAY_HINT_FADE_OUT_DURATION_MS)
            .alpha(0F)
    }

    private fun setupPlaylistButton() {
        val playlistItems = playerService?.viewModel?.playlist?.value?.first
        if (playlistItems != null) {
            togglePlaylistEnabled(playlistItems)
        }

        playlist.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_playlist)
        }
    }

    private fun togglePlaylistEnabled(playlistItems: List<PlaylistItem>) {
        playlist.isEnabled = playlistItems.size > AudioPlayerService.SINGLE_PLAYLIST_SIZE
    }

    private fun hideToolbar() {
        toolbarVisible = false

        (requireActivity() as AudioPlayerActivity).hideToolbar()
    }

    private fun showToolbar() {
        toolbarVisible = true

        (requireActivity() as AudioPlayerActivity).showToolbar()
    }
}
