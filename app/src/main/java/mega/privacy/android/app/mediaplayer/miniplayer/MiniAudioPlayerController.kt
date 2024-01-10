package mega.privacy.android.app.mediaplayer.miniplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST

/**
 * A helper class containing UI logic of mini player, it help us keep ManagerActivity clean.
 *
 * @param playerView the ExoPlayer view
 * @param onPlayerVisibilityChanged a callback for mini player view visibility change
 */
class MiniAudioPlayerController constructor(
    private val playerView: StyledPlayerView,
    private val onPlayerVisibilityChanged: (() -> Unit)? = null,
) : LifecycleEventObserver {
    private val context = playerView.context

    private val trackName = playerView.findViewById<TextView>(R.id.track_name)
    private val artistName = playerView.findViewById<TextView>(R.id.artist_name)

    private var serviceBound = false
    private var serviceGateway: MediaPlayerServiceGateway? = null

    private var metadataChangedJob: Job? = null
    private var sharingScope: CoroutineScope? = null

    /**
     * The parameter that determine the player view whether should be visible
     */
    var shouldVisible = false
        set(value) {
            field = value
            updatePlayerViewVisibility()
        }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway

                metadataChangedJob = sharingScope?.launch {
                    serviceGateway?.metadataUpdate()?.collect {
                        trackName.text = it.title ?: it.nodeName

                        if (it.artist != null) {
                            artistName.text = it.artist
                            artistName.isVisible = true
                        } else {
                            artistName.isVisible = false
                        }
                    }
                }
                setupPlayerView()
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

    init {
        audioPlayerPlaying.observeForever(audioPlayerPlayingObserver)

        playerView.findViewById<ImageButton>(R.id.close).setOnClickListener {
            serviceGateway?.stopPlayer()
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
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume(source)
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> return
        }
    }

    /**
     * The onResume function is called when Lifecycle event ON_RESUME
     *
     * @param owner LifecycleOwner
     */
    fun onResume(owner: LifecycleOwner) {
        if (sharingScope == null) {
            sharingScope = owner.lifecycleScope
        }
        setupPlayerView()
        playerView.onResume()
    }

    /**
     * The onPause function is called when Lifecycle event ON_PAUSE
     */
    fun onPause() {
        playerView.onPause()
    }

    /**
     * The onDestroy function is called when Lifecycle event ON_DESTROY
     */
    fun onDestroy() {
        audioPlayerPlaying.removeObserver(audioPlayerPlayingObserver)
        metadataChangedJob?.cancel()
        onAudioPlayerServiceStopped()
    }

    /**
     * Get height of the mini player view.
     *
     * @return height of the mini player view
     */
    fun playerHeight() = playerView.measuredHeight

    /**
     * The player view whether is visible.
     *
     * @return true is player view is visible, otherwise is false.
     */
    fun visible() = playerView.isVisible

    private fun updatePlayerViewVisibility() {
        playerView.isVisible = serviceGateway?.run {
            shouldVisible
        } ?: false
    }

    private fun onAudioPlayerServiceStopped() {
        serviceGateway = null

        updatePlayerViewVisibility()

        if (serviceBound) {
            serviceBound = false
            context.unbindService(connection)
        }

        onPlayerVisibilityChanged?.invoke()
    }

    private fun setupPlayerView() {
        updatePlayerViewVisibility()
        serviceGateway?.setupPlayerView(playerView = playerView)
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
