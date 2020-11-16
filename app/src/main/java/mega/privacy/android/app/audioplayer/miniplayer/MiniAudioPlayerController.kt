package mega.privacy.android.app.audioplayer.miniplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.AudioPlayerActivity
import mega.privacy.android.app.audioplayer.service.AudioPlayerService
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceBinder
import mega.privacy.android.app.audioplayer.service.Metadata
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST

private val audioPlayerPlaying = MutableLiveData<Boolean>()

class MiniAudioPlayerController(
    private val playerView: PlayerView,
    private val context: Context,
    private val onPlayerVisibilityChanged: Runnable,
) {
    private val trackName = playerView.findViewById<TextView>(R.id.track_name)
    private val artistName = playerView.findViewById<TextView>(R.id.artist_name)

    private var serviceBound = false
    private var playerService: AudioPlayerService? = null

    var shouldVisible = false
        set(value) {
            field = value

            updatePlayerViewVisibility()
        }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioPlayerServiceBinder) {
                playerService = service.service

                setupPlayerView(service.service.exoPlayer)
                service.service.metadata.observeForever(metadataObserver)

                if (isVisible()) {
                    onPlayerVisibilityChanged.run()
                }
            }
        }
    }

    private val audioPlayerPlayingObserver = Observer<Boolean> {
        if (!serviceBound && it) {
            serviceBound = true

            val playerServiceIntent = Intent(context, AudioPlayerService::class.java)
            playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
            context.bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        } else if (!it) {
            onAudioPlayerServiceStopped()
        }
    }

    private val metadataObserver = Observer<Metadata> {
        trackName.text = it.title ?: it.nodeName

        if (it.artist != null) {
            artistName.text = it.artist
            artistName.isVisible = true
        } else {
            artistName.isVisible = false
        }
    }

    init {
        audioPlayerPlaying.observeForever(audioPlayerPlayingObserver)

        playerView.findViewById<ImageButton>(R.id.close).setOnClickListener {
            playerService?.stopAudioPlayer()
        }

        playerView.setOnClickListener {
            val intent = Intent(context, AudioPlayerActivity::class.java)
            intent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
            context.startActivity(intent)
        }
    }

    fun onResume() {
        val service = playerService
        if (service != null) {
            setupPlayerView(service.exoPlayer)
        }
    }

    fun onDestroy() {
        audioPlayerPlaying.removeObserver(audioPlayerPlayingObserver)
        onAudioPlayerServiceStopped()
    }

    fun isVisible() = playerView.isVisible

    private fun updatePlayerViewVisibility() {
        playerView.isVisible = playerService != null && shouldVisible
    }

    private fun onAudioPlayerServiceStopped() {
        playerService?.metadata?.removeObserver(metadataObserver)
        playerService = null

        updatePlayerViewVisibility()

        if (serviceBound) {
            serviceBound = false
            context.unbindService(connection)
        }

        onPlayerVisibilityChanged.run()
    }

    private fun setupPlayerView(player: SimpleExoPlayer) {
        updatePlayerViewVisibility()
        playerView.player = player

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false

        playerView.showController()
    }

    companion object {
        fun notifyAudioPlayerPlaying(playing: Boolean) {
            audioPlayerPlaying.value = playing
        }
    }
}
