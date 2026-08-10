package mega.privacy.android.app.mediaplayer.miniplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import timber.log.Timber

/**
 * A helper class containing the mini-player UI logic.
 *
 * @param playerView the ExoPlayer view
 * @param onPlayerVisibilityChanged a callback for mini player view visibility change
 * @param mainDispatcher dispatcher used for the internal instance scope; override in tests with a test dispatcher
 */
class MiniAudioPlayerController(
    private val playerView: PlayerView,
    private val onPlayerVisibilityChanged: (() -> Unit)? = null,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : LifecycleEventObserver {
    private val context = playerView.context

    private val iconTintColor by lazy {
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black_white))
    }

    private val trackName = playerView.findViewById<TextView>(R.id.track_name)
    private val artistName = playerView.findViewById<TextView>(R.id.artist_name)

    private var serviceBound = false
    private var serviceGateway: MediaPlayerServiceGateway? = null

    // V2 fields
    private var v2MediaController: MediaController? = null
    private var v2ControllerFuture: ListenableFuture<MediaController>? = null
    private var isV2NotificationDismissed = false

    private val instanceScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private var metadataChangedJob: Job? = null
    private var sharingScope: CoroutineScope? = null
    private var _audioBinding = MutableStateFlow(false)
    val audioBinding: Flow<Boolean> = _audioBinding

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updateUIRegardingPlayback(state)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            applyTintToPlayPauseButton()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            if (v2MediaController != null) {
                trackName.text = mediaMetadata.title ?: ""
                val artist = mediaMetadata.artist?.toString()
                artistName.text = artist ?: ""
                artistName.isVisible = artist != null
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val controller = v2MediaController ?: return
            if (isV2NotificationDismissed) {
                isV2NotificationDismissed = false
                updateUIRegardingPlayback(controller.playbackState)
            }
        }
    }

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
         * Called after a successful bind with our LegacyAudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway

                metadataChangedJob = sharingScope?.launch {
                    serviceGateway?.metadataUpdate()
                        ?.catch { Timber.e(it, "Failed to collect metadata update") }
                        ?.collect {
                            trackName.text = it.title ?: it.nodeName

                            artistName.text = it.artist
                            artistName.isVisible = !it.artist.isNullOrEmpty()
                        }
                }
                serviceGateway?.addPlayerListener(playerListener)
                setupPlayerView()
                if (visible()) {
                    onPlayerVisibilityChanged?.invoke()
                    sharingScope?.launch {
                        _audioBinding.emit(true)
                    }
                }
            }
        }
    }

    // Legacy handler — restored to original, unaffected by V2.
    private fun onAudioPlayerPlayingChanged(playing: Boolean) {
        if (playing) {
            if (!serviceBound) {
                serviceBound = true
                val playerServiceIntent = Intent(context, LegacyAudioPlayerService::class.java)
                playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                context.bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
            }
        } else {
            onAudioPlayerServiceStopped()
        }
    }

    // V2-only handler — completely independent from the legacy handler.
    private fun onV2AudioPlayerPlayingChanged(playing: Boolean) {
        if (playing) {
            if (v2MediaController == null && v2ControllerFuture == null) {
                connectToV2Service()
            }
        } else {
            disconnectV2Service()
        }
    }

    private fun onV2NotificationDismissedChanged(dismissed: Boolean) {
        isV2NotificationDismissed = dismissed
        val state = v2MediaController?.playbackState ?: return
        updateUIRegardingPlayback(state)
    }

    init {
        instanceScope.launch {
            audioPlayerPlaying.filterNotNull()
                .catch { Timber.e(it, "Failed to collect audioPlayerPlaying") }
                .collect { onAudioPlayerPlayingChanged(it) }
        }
        instanceScope.launch {
            v2AudioPlayerPlaying.filterNotNull()
                .catch { Timber.e(it, "Failed to collect v2AudioPlayerPlaying") }
                .collect { onV2AudioPlayerPlayingChanged(it) }
        }
        instanceScope.launch {
            v2NotificationDismissed.filterNotNull()
                .catch { Timber.e(it, "Failed to collect v2NotificationDismissed") }
                .collect { onV2NotificationDismissedChanged(it) }
        }

        playerView.findViewById<ImageButton>(R.id.close)?.let {
            ImageViewCompat.setImageTintList(it, iconTintColor)
            it.setOnClickListener {
                if (v2MediaController != null) {
                    AudioPlayerService.stopAudioPlayer(context)
                } else {
                    serviceGateway?.stopPlayer()
                }
            }
        }

        applyTintToPlayPauseButton()

        playerView.setOnClickListener {
            if (!CallUtil.participatingInACall()) {
                if (v2MediaController != null) {
                    val intent = MegaActivity.getIntentWithExtraDestinations(
                        context,
                        listOf(AudioPlayerScreenNavKey(AudioPlayerScreenNavKey.RESUME_LAUNCH_ID))
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, AudioPlayerActivity::class.java)
                    intent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                    context.startActivity(intent)
                }
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
     * Set the coroutine scope used for emitting [audioBinding] and metadata updates.
     * Call from Compose when the controller is first composed so [onServiceConnected]
     * can emit even when lifecycle is INITIALIZED/STARTED (e.g. after returning from Search).
     */
    fun setCoroutineScope(scope: CoroutineScope) {
        if (sharingScope == null) {
            sharingScope = scope
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
        val controller = v2MediaController
        if (controller != null) {
            setupPlayerViewV2(controller)
        } else {
            setupPlayerView()
        }
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
        instanceScope.cancel()
        // V2 cleanup — no-op if V2 was never connected.
        runCatching { v2MediaController?.removeListener(playerListener) }
            .onFailure { Timber.e(it, "Failed to remove V2 player listener") }
        val future = v2ControllerFuture
        v2ControllerFuture = null
        v2MediaController = null
        runCatching { future?.let { MediaController.releaseFuture(it) } }
            .onFailure { Timber.e(it, "Failed to release V2 MediaController") }
        // Legacy cleanup — no-op if legacy was never bound.
        serviceGateway?.removeListener(playerListener)
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
        playerView.isVisible =
            (v2MediaController != null || serviceGateway != null) && shouldVisible
    }

    private fun onAudioPlayerServiceStopped() {
        serviceGateway = null

        updatePlayerViewVisibility()

        if (serviceBound) {
            serviceBound = false
            context.unbindService(connection)
        }

        onPlayerVisibilityChanged?.invoke()
        sharingScope?.launch { _audioBinding.emit(false) }
    }

    private fun setupPlayerView() {
        updatePlayerViewVisibility()
        serviceGateway?.getPlaybackState()?.let { state ->
            updateUIRegardingPlayback(state)
        }
        serviceGateway?.setupPlayerView(playerView = playerView)
    }

    private fun connectToV2Service() {
        val sessionToken = runCatching {
            SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        }.getOrElse {
            Timber.e(it, "Failed to create SessionToken for AudioPlayerService")
            return
        }
        v2ControllerFuture =
            MediaController.Builder(context, sessionToken).buildAsync().also { future ->
                Futures.addCallback(
                    future,
                    object : FutureCallback<MediaController> {
                        override fun onSuccess(result: MediaController) {
                            // Guard against delivery after release was already called.
                            if (v2ControllerFuture == null) {
                                result.release()
                                return
                            }
                            v2MediaController = result
                            result.addListener(playerListener)
                            setupPlayerViewV2(result)
                            if (visible()) {
                                onPlayerVisibilityChanged?.invoke()
                                sharingScope?.launch { _audioBinding.emit(true) }
                            }
                        }

                        override fun onFailure(t: Throwable) {
                            Timber.e(t, "Failed to connect MediaController to AudioPlayerService")
                            v2ControllerFuture = null
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setupPlayerViewV2(controller: MediaController) {
        playerView.player = controller
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false
        updatePlayerViewVisibility()
        updateUIRegardingPlayback(controller.playbackState)
        val metadata = controller.mediaMetadata
        trackName.text = metadata.title ?: ""
        val artist = metadata.artist?.toString()
        artistName.text = artist ?: ""
        artistName.isVisible = artist != null
        applyTintToPlayPauseButton()
        playerView.showController()
    }

    private fun disconnectV2Service() {
        v2MediaController?.removeListener(playerListener)
        val future = v2ControllerFuture
        v2ControllerFuture = null
        v2MediaController = null
        runCatching { future?.let { MediaController.releaseFuture(it) } }
            .onFailure { Timber.e(it, "Failed to release V2 MediaController") }
        updatePlayerViewVisibility()
        onPlayerVisibilityChanged?.invoke()
        sharingScope?.launch { _audioBinding.emit(false) }
    }

    private fun updateUIRegardingPlayback(state: Int) {
        playerView.findViewById<View>(R.id.loading_mini_audio_player).isVisible =
            state == Player.STATE_BUFFERING && !isV2NotificationDismissed
        playerView.findViewById<View>(R.id.play_pause_placeholder).visibility =
            if (state > Player.STATE_BUFFERING || isV2NotificationDismissed)
                View.VISIBLE
            else
                View.INVISIBLE
    }

    private fun applyTintToPlayPauseButton() {
        val playPauseButton = playerView.findViewById<ImageButton>(R.id.exo_play_pause) ?: return
        ImageViewCompat.setImageTintList(playPauseButton, iconTintColor)
    }

    companion object {
        private val audioPlayerPlaying = MutableStateFlow<Boolean?>(null)
        private val v2AudioPlayerPlaying = MutableStateFlow<Boolean?>(null)
        private val v2NotificationDismissed = MutableStateFlow<Boolean?>(null)

        /**
         * Notify if the legacy audio player is playing or closed.
         *
         * @param playing true if player is playing, false if player is closed
         */
        fun notifyAudioPlayerPlaying(playing: Boolean) {
            audioPlayerPlaying.value = playing
        }

        /**
         * Notify if the V2 (Compose/Media3) audio player is playing or closed.
         *
         * @param playing true if player is playing, false if player is closed
         */
        fun notifyV2AudioPlayerPlaying(playing: Boolean) {
            v2AudioPlayerPlaying.value = playing
        }

        /**
         * Notify when the V2 audio player notification is dismissed or re-shown.
         *
         * @param dismissed true when the notification has been swiped away (service still running),
         *                  false when the player resumes or a new item starts.
         */
        fun notifyV2NotificationDismissed(dismissed: Boolean) {
            v2NotificationDismissed.value = dismissed
        }
    }
}
