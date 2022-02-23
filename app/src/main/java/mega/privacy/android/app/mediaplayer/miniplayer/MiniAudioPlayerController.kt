package mega.privacy.android.app.mediaplayer.miniplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.google.android.exoplayer2.ui.PlayerView
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.service.*
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST

/**
 * A helper class containing UI logic of mini player, it help us keep ManagerActivity clean.
 *
 * @param playerView the ExoPlayer view
 * @param onPlayerVisibilityChanged a callback for mini player view visibility change
 */
@Suppress("DEPRECATION")
class MiniAudioPlayerController constructor(
    private val playerView: PlayerView,
    private val onPlayerVisibilityChanged: (() -> Unit)? = null,
) : LifecycleEventObserver {
    private val context = playerView.context

    private val trackName = playerView.findViewById<TextView>(R.id.track_name)
    private val artistName = playerView.findViewById<TextView>(R.id.artist_name)

    private var serviceBound = false
    private var playerService: MediaPlayerService? = null

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
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                setupPlayerView(service.service.player)
                service.service.metadata.observeForever(metadataObserver)

                if (visible()) {
                    onPlayerVisibilityChanged?.invoke()
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
            if (!CallUtil.participatingInACall()) {
                val intent = Intent(context, AudioPlayerActivity::class.java)
                intent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                context.startActivity(intent)
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event){
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> return
        }
    }

    fun onResume() {
        val service = playerService
        if (service != null) {
            setupPlayerView(service.player)
        }
        playerView.onResume()
    }

    fun onPause() {
        playerView.onPause()
    }

    fun onDestroy() {
        audioPlayerPlaying.removeObserver(audioPlayerPlayingObserver)
        onAudioPlayerServiceStopped()
    }

    /**
     * Get height of the mini player view.
     *
     * @return height of the mini player view
     */
    fun playerHeight() = playerView.measuredHeight

    fun visible() = playerView.isVisible

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

        onPlayerVisibilityChanged?.invoke()
    }

    private fun setupPlayerView(player: MediaMegaPlayer) {
        updatePlayerViewVisibility()
        playerView.player = player

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false

        playerView.showController()
    }

    companion object {
        private val audioPlayerPlaying = MutableLiveData<Boolean>()

        /**
         * Notify if audio player is playing or closed.
         *
         * @param playing true if player is playing, false if player is closed
         */
        fun notifyAudioPlayerPlaying(playing: Boolean) {
            audioPlayerPlaying.value = playing
        }
    }


}
