package mega.privacy.android.app.mediaplayer.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.mediaplayer.AudioPlayQueueBuilder
import mega.privacy.android.app.mediaplayer.mapper.ExoPlayerRepeatModeMapper
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.AudioPlayQueueParams
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.TrackAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.mobile.analytics.event.AudioPlayerIsActivatedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Audio player service using [MediaSessionService] (Media3-recommended approach).
 *
 * Audio focus and headset becoming-noisy events are handled automatically by ExoPlayer
 * when built with [AudioAttributes.DEFAULT] and handleAudioFocus/handleAudioBecomingNoisy enabled.
 * The system notification is managed automatically via [MediaSessionService].
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class AudioPlayerService : MediaSessionService(), LifecycleEventObserver {

    @Inject
    lateinit var audioPlayQueueBuilder: AudioPlayQueueBuilder

    @Inject
    lateinit var trackAudioPlaybackInfoUseCase: TrackAudioPlaybackInfoUseCase

    @Inject
    lateinit var monitorAudioBackgroundPlayEnabledUseCase: MonitorAudioBackgroundPlayEnabledUseCase

    @Inject
    lateinit var monitorAudioShuffleEnabledUseCase: MonitorAudioShuffleEnabledUseCase

    @Inject
    lateinit var monitorAudioRepeatModeUseCase: MonitorAudioRepeatModeUseCase

    @Inject
    lateinit var megaApiHttpServerStopUseCase: MegaApiHttpServerStopUseCase

    @Inject
    lateinit var megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase

    @Inject
    lateinit var exoPlayerRepeatModeMapper: ExoPlayerRepeatModeMapper

    @Inject
    lateinit var monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase

    @Inject
    lateinit var monitorAccountDetailUseCase: MonitorAccountDetailUseCase

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private var isNotificationDismissed = false
    private var needPlayWhenGoForeground = false
    private var isBackgroundPlayEnabled = true
    private var needStopStreamingServer = false
    private var playQueueBuildJob: Job? = null
    private var trackPlaybackInfoJob: Job? = null
    private var lastPlayQueueParams: AudioPlayQueueParams? = null

    override fun onCreate() {
        super.onCreate()
        Analytics.tracker.trackEvent(AudioPlayerIsActivatedEvent)
        initializePlayer()
        initializeMediaNotificationProvider()
        initializeSession()
        observePreferences()
        observeHiddenNodeSettingChanges()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(createPlayerListener())
                // Apply persisted shuffle/repeat preferences asynchronously.
                lifecycleScope.launch {
                    val shuffleEnabled = monitorAudioShuffleEnabledUseCase()
                        .catch { Timber.e(it, "Failed to load shuffle preference") }
                        .firstOrNull() ?: false
                    val repeatMode = monitorAudioRepeatModeUseCase()
                        .catch { Timber.e(it, "Failed to load repeat mode preference") }
                        .firstOrNull()
                    exoPlayer.shuffleModeEnabled = shuffleEnabled
                    if (repeatMode != null) {
                        exoPlayer.repeatMode = exoPlayerRepeatModeMapper(repeatMode)
                    }
                }
            }
    }

    /**
     * Replaces the default Media3 notification provider with one that sets a custom
     * [android.app.Notification.deleteIntent] when the player is paused.
     *
     * Without this, swiping the notification while paused causes Media3 to re-post it
     * immediately (the default deleteIntent triggers an internal update cycle that re-posts
     * the notification). Our deleteIntent sends ACTION_DISMISS directly to [onStartCommand],
     * which removes the notification and sets the dismissed flag — keeping the service alive
     * in a paused state without re-posting the notification.
     */
    private fun initializeMediaNotificationProvider() {
        val dismissDeleteIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioPlayerService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val defaultProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback,
            ): MediaNotification {
                val notification = defaultProvider.createNotification(
                    session, customLayout, actionFactory, onNotificationChangedCallback
                )
                if (!session.player.isPlaying) {
                    notification.notification.deleteIntent = dismissDeleteIntent
                }
                return notification
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle,
            ): Boolean = defaultProvider.handleCustomCommand(session, action, extras)

            override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
                defaultProvider.notificationChannelInfo
        })
    }

    private fun initializeSession() {
        val pendingIntent = MegaActivity.getPendingIntentWithExtraDestination(
            this,
            AudioPlayerScreenNavKey(AudioPlayerScreenNavKey.RESUME_LAUNCH_ID)
        )
        val currentPlayer = player ?: return
        mediaSession = MediaSession.Builder(this, currentPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun observePreferences() {
        lifecycleScope.launch {
            monitorAudioBackgroundPlayEnabledUseCase()
                .catch { Timber.e(it, "Failed to monitor background play preference") }
                .collect { isBackgroundPlayEnabled = it }
        }
    }

    private fun observeHiddenNodeSettingChanges() {
        lifecycleScope.launch {
            combine(
                monitorShowHiddenItemsUseCase(),
                monitorAccountDetailUseCase(),
            ) { showHidden, accountDetail -> showHidden to accountDetail }
                .drop(1) // Skip the initial emission — play queue already built on first load.
                .catch { Timber.e(it, "Failed to monitor hidden node settings") }
                .collect { lastPlayQueueParams?.let { params -> rebuildPlayQueue(params) } }
        }
    }

    private fun createPlayerListener(): Player.Listener = object : Player.Listener {

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) {
                player?.shuffleOrder = ShuffleOrder.DefaultShuffleOrder(
                    player?.mediaItemCount ?: 0,
                    System.currentTimeMillis()
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            isNotificationDismissed = false
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                isNotificationDismissed = false
                MiniAudioPlayerController.notifyV2NotificationDismissed(false)
            } else {
                // Remove the foreground lock so the notification can be swiped away when
                // the player is paused. MediaSessionService will re-enter foreground
                // automatically if playback resumes.
                ServiceCompat.stopForeground(
                    this@AudioPlayerService,
                    ServiceCompat.STOP_FOREGROUND_DETACH
                )
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            player?.prepare()
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForeground: Boolean) {
        if (isNotificationDismissed && !session.player.isPlaying) return
        if (session.player.isPlaying) isNotificationDismissed = false
        super.onUpdateNotification(session, startInForeground)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle our custom actions before calling super to prevent MediaSessionService from
        // triggering onUpdateNotification internally, which can cause unexpected player state
        // changes (e.g. brief STATE_BUFFERING) as a side-effect of its startForeground logic.
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayerAndSelf()
                return START_NOT_STICKY
            }

            ACTION_DISMISS -> {
                isNotificationDismissed = true
                MiniAudioPlayerController.notifyV2NotificationDismissed(true)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                return START_NOT_STICKY
            }
        }
        super.onStartCommand(intent, flags, startId)
        val safeIntent = intent ?: return START_STICKY
        if (!safeIntent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            return START_STICKY
        }
        val params = AudioPlayQueueParams.from(safeIntent) ?: return START_STICKY
        // Accumulate the needStopHttpServer flag from intents across play queue rebuilds.
        needStopStreamingServer = needStopStreamingServer || params.needStopHttpServer
        lastPlayQueueParams = params
        rebuildPlayQueue(params)
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(true)
        trackPlaybackInfoJob?.cancel()
        trackPlaybackInfoJob = lifecycleScope.launch {
            player?.let { exoPlayer ->
                trackAudioPlaybackInfoUseCase {
                    val handle = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
                    MediaPlaybackInfo(
                        mediaHandle = handle,
                        totalDuration = exoPlayer.duration.coerceAtLeast(0L),
                        currentPosition = exoPlayer.currentPosition,
                        mediaType = MediaType.Audio,
                    )
                }
            }
        }
        return START_STICKY
    }

    private fun rebuildPlayQueue(params: AudioPlayQueueParams) {
        playQueueBuildJob?.cancel()
        playQueueBuildJob = lifecycleScope.launch {
            audioPlayQueueBuilder(params)
                .catch { Timber.e(it, "Failed to build audio play queue") }
                .collect { mediaSources ->
                    needStopStreamingServer =
                        needStopStreamingServer || mediaSources.serverStarted
                    applyMediaSources(mediaSources)
                }
        }
    }

    private fun applyMediaSources(mediaSources: MediaPlaySources) {
        val currentPlayer = player ?: return
        if (mediaSources.mediaItems.isEmpty()) return

        val startIndex = mediaSources.newIndexForCurrentItem.coerceAtLeast(0)
        val targetMediaId = mediaSources.mediaItems.getOrNull(startIndex)?.mediaId

        // When the full play queue update arrives while the target item is already
        // buffering or playing as a single placeholder, expand the play queue around
        // it using addMediaItems so current playback is not interrupted at all.
        // setMediaItems always resets the player to STATE_IDLE (even with a preserved
        // position), which causes an audible re-buffer / restart for cached files.
        if (targetMediaId != null
            && currentPlayer.mediaItemCount == 1
            && currentPlayer.currentMediaItem?.mediaId == targetMediaId
            && currentPlayer.playbackState != Player.STATE_IDLE
        ) {
            if (startIndex > 0) {
                currentPlayer.addMediaItems(0, mediaSources.mediaItems.subList(0, startIndex))
            }
            val afterStart = startIndex + 1
            if (afterStart < mediaSources.mediaItems.size) {
                currentPlayer.addMediaItems(
                    afterStart,
                    mediaSources.mediaItems.subList(afterStart, mediaSources.mediaItems.size)
                )
            }
        } else {
            currentPlayer.setMediaItems(mediaSources.mediaItems, startIndex, 0L)
            currentPlayer.prepare()
            currentPlayer.play()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (needPlayWhenGoForeground) {
                    player?.play()
                    needPlayWhenGoForeground = false
                }
            }

            Lifecycle.Event.ON_STOP -> {
                if (!isBackgroundPlayEnabled) {
                    val currentPlayer = player ?: return
                    if (currentPlayer.isPlaying) {
                        currentPlayer.pause()
                        needPlayWhenGoForeground = true
                    }
                }
            }

            else -> {}
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayerAndSelf()
    }

    private fun stopPlayerAndSelf() {
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // Release the session before stopSelf() so that all bound MediaController clients
        // (including the Audio Player screen) disconnect immediately. Without this, stopSelf()
        // cannot stop the service while clients are still bound, which causes MediaSessionService
        // to re-post the notification before the service actually dies.
        runCatching { mediaSession?.release() }
            .onFailure { Timber.e(it, "Failed to release MediaSession") }
        mediaSession = null
        stopSelf()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        playQueueBuildJob?.cancel()
        trackPlaybackInfoJob?.cancel()
        if (needStopStreamingServer) {
            lifecycleScope.launch {
                runCatching { megaApiHttpServerStopUseCase() }
                    .onFailure { Timber.e(it, "Failed to stop HTTP server") }
                runCatching { megaApiFolderHttpServerStopUseCase() }
                    .onFailure { Timber.e(it, "Failed to stop folder HTTP server") }
            }
        }
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false)
        runCatching { mediaSession?.release() }
            .onFailure { Timber.e(it, "Failed to release MediaSession") }
        mediaSession = null
        runCatching { player?.release() }
            .onFailure { Timber.e(it, "Failed to release ExoPlayer") }
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "mega.privacy.android.app.mediaplayer.AudioPlayerService.STOP"
        private const val ACTION_DISMISS = "mega.privacy.android.app.mediaplayer.AudioPlayerService.DISMISS"

        /**
         * Stops the audio player service from outside a bound component.
         *
         * Uses a start-service intent with ACTION_STOP so the service can run
         * stopPlayerAndSelf() internally: this first calls
         * MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false) (which releases the mini
         * player's MediaController binding), then calls stopSelf(). Once all bindings are
         * released the service is destroyed. Calling stopService() directly would deadlock
         * because a started+bound service is not destroyed until all clients unbind — and the
         * mini player's MediaController only unbinds after receiving the notification.
         */
        fun stopAudioPlayer(context: Context) {
            context.startService(
                Intent(context, AudioPlayerService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
