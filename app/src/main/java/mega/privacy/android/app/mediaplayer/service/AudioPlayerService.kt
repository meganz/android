package mega.privacy.android.app.mediaplayer.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.app.mediaplayer.AudioPlayQueueBuilder
import mega.privacy.android.app.mediaplayer.AudioPlayQueueFilterDiff
import mega.privacy.android.app.mediaplayer.PlayedPastThresholdTracker
import mega.privacy.android.app.mediaplayer.isStreamingOverQuotaError
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.AudioPlayQueueParams
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioResumePositionUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.PersistAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.TrackAndSaveAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodeAccessibilityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStreamOverQuotaEventUseCase
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerError
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerErrorEventQueue
import mega.privacy.android.feature.mediaplayer.data.AudioResumeEvent
import mega.privacy.android.feature.mediaplayer.data.AudioResumeEventQueue
import mega.privacy.android.feature.mediaplayer.data.MediaHandleStore
import mega.privacy.android.feature.mediaplayer.data.mapper.ExoPlayerRepeatModeMapper
import mega.privacy.android.feature.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.mobile.analytics.event.AudioPlayerIsActivatedEvent
import timber.log.Timber

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
    lateinit var trackAndSaveAudioPlaybackInfoUseCase: TrackAndSaveAudioPlaybackInfoUseCase

    @Inject
    lateinit var persistAudioPlaybackInfoUseCase: PersistAudioPlaybackInfoUseCase

    @Inject
    lateinit var getAudioResumePositionUseCase: GetAudioResumePositionUseCase

    /**
     * Forced playback-info saves run on the application scope, not [lifecycleScope]: the ones
     * fired while the service is being torn down (stop, task removed) must outlive it.
     */
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var monitorAudioBackgroundPlayEnabledUseCase: MonitorAudioBackgroundPlayEnabledUseCase

    @Inject
    lateinit var monitorAudioShuffleEnabledUseCase: MonitorAudioShuffleEnabledUseCase

    @Inject
    lateinit var monitorAudioRepeatModeUseCase: MonitorAudioRepeatModeUseCase

    @Inject
    lateinit var monitorAudioContinuousPlaybackEnabledUseCase: MonitorAudioContinuousPlaybackEnabledUseCase

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

    @Inject
    lateinit var mediaHandleStore: MediaHandleStore

    @Inject
    lateinit var audioResumeEventQueue: AudioResumeEventQueue

    @Inject
    lateinit var monitorStreamOverQuotaEventUseCase: MonitorStreamOverQuotaEventUseCase

    @Inject
    lateinit var getBusinessStatusUseCase: GetBusinessStatusUseCase

    @Inject
    lateinit var checkNodeAccessibilityUseCase: CheckNodeAccessibilityUseCase

    @Inject
    lateinit var isConnectedToInternetUseCase: IsConnectedToInternetUseCase

    @Inject
    lateinit var isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase

    @Inject
    lateinit var audioPlayerErrorEventQueue: AudioPlayerErrorEventQueue

    @Inject
    lateinit var isInTransferOverQuotaUseCase: IsInTransferOverQuotaUseCase

    @Inject
    lateinit var transferOverQuotaEventQueue: TransferOverQuotaEventQueue

    /**
     * Disk cache shared with the legacy players; null when the cache directory could not be
     * initialised, in which case streaming reads go straight upstream. The cache is owned by the
     * singleton component and must never be released here.
     */
    @Inject
    @JvmField
    var simpleCache: SimpleCache? = null

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private var isNotificationDismissed = false
    private var needPlayWhenGoForeground = false
    private var isBackgroundPlayEnabled = true
    private var needStopStreamingServer = false
    private var playQueueBuildJob: Job? = null
    private var trackPlaybackInfoJob: Job? = null
    private var lastPlayQueueParams: AudioPlayQueueParams? = null

    // Announce the applied resume only when the rebuild opens a different item; rebuilds of
    // the queue around the already-playing item are not a resume from the user's point of view.
    private var shouldAnnounceResume = false

    // Latest snapshot with a resolved handle, kept because forced saves on track change fire
    // after the player has already moved to the next item and its duration is no longer readable.
    private var lastPlaybackInfo: MediaPlaybackInfo? = null

    private val playedPastThresholdTracker = PlayedPastThresholdTracker()
    private var isTransferOverQuota = false
    private var playbackRetryCount = 0
    private var retryJob: Job? = null
    private var absorbJob: Job? = null
    private var pausedByExternalCommand = false

    private val cacheEventListener = object : CacheDataSource.EventListener {
        override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
            Timber.d("[Cache] HIT: read ${cachedBytesRead / 1024} KB from disk (cache total: ${cacheSizeBytes / 1024 / 1024} MB)")
        }

        override fun onCacheIgnored(reason: Int) {
            val reasonText = when (reason) {
                CacheDataSource.CACHE_IGNORED_REASON_ERROR -> "cache read/write error, falling back to upstream"
                CacheDataSource.CACHE_IGNORED_REASON_UNSET_LENGTH -> "unset length, cache bypassed"
                else -> "unknown reason ($reason)"
            }
            Timber.d("[Cache] IGNORED: $reasonText")
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Analytics.tracker.trackEvent(AudioPlayerIsActivatedEvent)
        initializePlayer()
        initializeMediaNotificationProvider()
        initializeSession()
        observePreferences()
        observeHiddenNodeSettingChanges()
        observeTransferOverQuota()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(buildDataSourceFactory())
                    .setLoadErrorHandlingPolicy(
                        DefaultLoadErrorHandlingPolicy(LOAD_ERROR_RETRY_COUNT)
                    )
            )
            .setLoadControl(buildLoadControl())
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(createPlayerListener())
                // Apply the persisted shuffle preference asynchronously. Repeat mode is applied
                // by observePreferences(), together with continuous playback.
                lifecycleScope.launch {
                    exoPlayer.shuffleModeEnabled = monitorAudioShuffleEnabledUseCase()
                        .catch { Timber.e(it, "Failed to load shuffle preference") }
                        .firstOrNull() ?: false
                }
            }
    }

    /**
     * Builds the streaming stack with the same characteristics as the legacy player: 5 s HTTP
     * timeouts so weak-network failures surface quickly into the playback-error flow, wrapped in
     * the shared disk cache so replaying a streamed track is served from disk instead of the
     * network. Falls back to the plain upstream factory when the cache is unavailable.
     */
    private fun buildDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(HTTP_TIMEOUT_MS)
                .setReadTimeoutMs(HTTP_TIMEOUT_MS),
        )
        val cache = simpleCache ?: return upstreamFactory
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(cacheEventListener)
    }

    private fun buildLoadControl(): DefaultLoadControl {
        val isLowRam =
            getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
        return if (isLowRam) {
            DefaultLoadControl.Builder()
                .setTargetBufferBytes(LOW_RAM_TARGET_BUFFER_BYTES)
                .setPrioritizeTimeOverSizeThresholds(false)
                .setBufferDurationsMs(
                    LOW_RAM_MIN_BUFFER_MS,
                    LOW_RAM_MAX_BUFFER_MS,
                    LOW_RAM_BUFFER_FOR_PLAYBACK_MS,
                    LOW_RAM_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                )
                .build()
        } else {
            DefaultLoadControl() // Normal devices: use ExoPlayer default buffers (50 s)
        }
    }

    /**
     * Applies the persisted repeat mode and continuous-playback preferences to the player.
     *
     * The preferences are the single source of truth for both: this only reads them, so the
     * invariants between the two are left to the use cases that write them.
     *
     * [ExoPlayer.setPauseAtEndOfMediaItems] acts independently of the repeat mode -- it pauses at
     * the end of every item whatever the mode is -- so it may only be used to stop after the
     * current track when no repeat mode is asking playback to carry on.
     */
    private fun applyPlaybackMode(
        repeatMode: RepeatToggleMode,
        continuousPlaybackEnabled: Boolean,
    ) {
        val exoPlayer = player ?: return
        val effectiveMode =
            if (repeatMode == RepeatToggleMode.REPEAT_ALL && !continuousPlaybackEnabled) {
                RepeatToggleMode.REPEAT_NONE
            } else {
                repeatMode
            }
        exoPlayer.repeatMode = exoPlayerRepeatModeMapper(effectiveMode)
        exoPlayer.setPauseAtEndOfMediaItems(
            !continuousPlaybackEnabled && effectiveMode == RepeatToggleMode.REPEAT_NONE
        )
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
        // The player screen opened by a file tap carries a different launch id, so without the
        // drop option tapping the notification would stack a second player (and a second
        // ViewModel, losing e.g. the sleep timer) on top of the one already showing.
        val pendingIntent = MegaActivity.getPendingIntentWithExtraDestination(
            this,
            AudioPlayerScreenNavKey(AudioPlayerScreenNavKey.RESUME_LAUNCH_ID),
            dropIfAlreadyShown = true,
        )
        val currentPlayer = player ?: return
        mediaSession = MediaSession.Builder(this, currentPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    if (!isTrustedController(controller)) {
                        return MediaSession.ConnectionResult.reject()
                    }
                    return super.onConnect(session, controller)
                }
            })
            .build()
    }

    private fun isTrustedController(controllerInfo: MediaSession.ControllerInfo): Boolean =
        isPackageTrusted(controllerInfo.packageName, packageName)

    private fun observePreferences() {
        lifecycleScope.launch {
            monitorAudioBackgroundPlayEnabledUseCase()
                .catch { Timber.e(it, "Failed to monitor background play preference") }
                .collect { isBackgroundPlayEnabled = it }
        }
        lifecycleScope.launch {
            combine(
                monitorAudioRepeatModeUseCase(),
                monitorAudioContinuousPlaybackEnabledUseCase(),
            ) { repeatMode, continuousPlaybackEnabled ->
                repeatMode to continuousPlaybackEnabled
            }
                .catch { Timber.e(it, "Failed to monitor playback mode preferences") }
                .collect { (repeatMode, continuousPlaybackEnabled) ->
                    applyPlaybackMode(repeatMode, continuousPlaybackEnabled)
                }
        }
    }

    private data class HiddenNodesFilterKey(
        val showHiddenItems: Boolean,
        val accountType: AccountType?,
        val isBusinessExpired: Boolean,
    )

    private fun observeHiddenNodeSettingChanges() {
        lifecycleScope.launch {
            combine(
                monitorShowHiddenItemsUseCase(),
                monitorAccountDetailUseCase(),
            ) { showHidden, accountDetail ->
                val accountType = accountDetail.levelDetail?.accountType
                HiddenNodesFilterKey(
                    showHiddenItems = showHidden,
                    accountType = accountType,
                    isBusinessExpired = accountType?.isBusinessAccount == true &&
                            runCatching { getBusinessStatusUseCase() }
                                .getOrNull() == BusinessAccountStatus.Expired,
                )
            }
                // Only a change in the actual filter inputs justifies a rebuild — AccountDetail
                // re-emits for unrelated reasons (storage usage updates etc.), and rebuilding on
                // each of those would keep interrupting playback mid-session.
                .distinctUntilChanged()
                .drop(1) // Skip the initial emission — play queue already built on first load.
                .catch { Timber.e(it, "Failed to monitor hidden node settings") }
                .collect {
                    lastPlayQueueParams?.let { params -> rebuildPlayQueueForFilterChange(params) }
                }
        }
    }

    /**
     * Streaming over quota means playback cannot continue, so it is stopped instead of retried:
     * nothing re-requests the stream until the user asks for it, which is what stops the warning
     * from reappearing as soon as it is dismissed.
     *
     * The SDK's per-request stream event is observed rather than the broadcast over-quota state:
     * the state is backed by a state flow that deduplicates values and is never reset while
     * logged in, so a second quota hit in the same session would not be delivered through it.
     * Reacting to the event also aborts the load before ExoPlayer's error-handling policy retries
     * the request, which would make the SDK raise the warning again.
     */
    private fun observeTransferOverQuota() {
        lifecycleScope.launch {
            monitorStreamOverQuotaEventUseCase()
                .catch { Timber.e(it, "Failed to monitor streaming over quota events") }
                .collect {
                    isTransferOverQuota = true
                    enterOverQuotaPausedState()
                }
        }
    }

    /**
     * The player is stopped while over quota, so pressing play (in the player screen or the
     * notification) cannot resume on its own. Ask the SDK whether the quota window has passed:
     * if it has, prepare the stream again, otherwise stay stopped and warn the user. Pressing
     * play is a deliberate action, so the warning is raised every time rather than only once
     * per quota window.
     */
    private fun handlePlayRequestedWhileOverQuota() {
        lifecycleScope.launch {
            val stillOverQuota = runCatching { isInTransferOverQuotaUseCase() }
                .onFailure { Timber.e(it) }
                .getOrDefault(true)
            if (stillOverQuota) {
                enterOverQuotaPausedState()
                transferOverQuotaEventQueue.emit(TransferOverQuotaSource.Streaming)
            } else {
                isTransferOverQuota = false
                player?.prepare()
            }
        }
    }

    /**
     * Pauses playback for the over-quota window and hides the mini player: the mini player exists
     * to keep controlling ongoing playback, but while over quota playback cannot continue, so an
     * idle bar would only take up space. It is shown again once playback actually resumes.
     *
     * The player is stopped, not just paused: pausing keeps ExoPlayer loading the stream, which
     * keeps the UI in a buffering state that can never finish and keeps re-requesting the stream,
     * making the SDK raise the over-quota warning again. stop() aborts the loads and moves the
     * player to idle while keeping the play queue and position, so prepare() can resume later.
     *
     * pause() before stop() is still required: stop() keeps playWhenReady as it is, and without
     * resetting it the next play press would not produce the playWhenReady transition that
     * re-checks the quota (see onPlayWhenReadyChanged).
     */
    private fun enterOverQuotaPausedState() {
        // Kill any pending retry backoff: its delayed prepare() would re-request the stream
        // right after this stop and re-raise the over-quota warning.
        retryJob?.cancel()
        player?.pause()
        player?.stop()
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false)
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
                pausedByExternalCommand = false
                isNotificationDismissed = false
                MiniAudioPlayerController.notifyV2NotificationDismissed(false)
                // Reconnect the mini player on every real resume: it is released while over
                // quota, and resuming after the window passes goes through prepare() (see
                // handlePlayRequestedWhileOverQuota), which does not re-show it itself.
                MiniAudioPlayerController.notifyV2AudioPlayerPlaying(true)
            } else {
                forceSaveCurrentPlaybackInfo()
                // Remove the foreground lock so the notification can be swiped away when the
                // player is paused. Only on a real pause: isPlaying also drops while audio focus
                // is suppressed (a phone call) or the player rebuffers, with playWhenReady still
                // true. Media3 keeps the service in the foreground for those and only re-promotes
                // on playback-state / playWhenReady changes, so detaching here would leave a
                // player that resumes after the call running as a background service, which the
                // system stops shortly after.
                if (player?.playWhenReady == false) {
                    ServiceCompat.stopForeground(
                        this@AudioPlayerService,
                        ServiceCompat.STOP_FOREGROUND_DETACH
                    )
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            when (reason) {
                Player.DISCONTINUITY_REASON_SEEK,
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION,
                Player.DISCONTINUITY_REASON_SKIP,
                    -> if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) {
                    // A seek, or a repeat-one loop. A loop restarts near zero, which the persist
                    // use case discards as below the minimum playback threshold: the user is
                    // still listening, so the item must stay resumable rather than count as
                    // played through.
                    forceSaveCurrentPlaybackInfo()
                } else {
                    saveLeftItemPlaybackInfo(oldPosition)
                }

                // Media3 only reports REMOVE when the playing item itself left the timeline, so
                // the item being left is always the old one even when the successor inherits its
                // index (e.g. both at 0). Comparing indices here would misfile that as a seek.
                Player.DISCONTINUITY_REASON_REMOVE -> saveLeftItemPlaybackInfo(oldPosition)

                else -> {}
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) return
            // Playback requests that bypass the player screen (Bluetooth buttons, the
            // notification) must not grab audio focus from an ongoing MEGA call.
            lifecycleScope.launch {
                val inCall = runCatching { isParticipatingInChatCallUseCase() }
                    .onFailure { Timber.e(it, "Failed to check ongoing call state") }
                    .getOrDefault(false)
                if (inCall) {
                    // Mark the pause as external: the brief isPlaying=true from this blocked
                    // attempt has already cleared the flag set by ACTION_PAUSE, and without
                    // re-arming it the post-call ACTION_RESUME would refuse to restore playback.
                    pausedByExternalCommand = true
                    player?.pause()
                }
            }
            when {
                isTransferOverQuota -> handlePlayRequestedWhileOverQuota()
                player?.playbackState == Player.STATE_IDLE -> {
                    // Pressing play after a terminal error is a deliberate action: grant a
                    // fresh retry budget instead of failing immediately at the exhausted count.
                    playbackRetryCount = 0
                    player?.prepare()
                }
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) playbackRetryCount = 0
        }

        override fun onPlayerError(error: PlaybackException) {
            if (isTransferOverQuota) {
                enterOverQuotaPausedState()
                return
            }
            // When over quota, playback is stopped instead of re-prepared — retries can never
            // succeed, and pressing play re-checks the quota and re-raises the warning
            // (see handlePlayRequestedWhileOverQuota).
            retryJob = lifecycleScope.launch {
                val isOverQuota =
                    isStreamingOverQuotaError(error.errorCode, isInTransferOverQuotaUseCase)
                // The flag is re-checked after the suspending query: the over-quota event can
                // arrive while the query is in flight, and prepare() would undo the stop it
                // performed.
                when {
                    isOverQuota || isTransferOverQuota -> {
                        isTransferOverQuota = true
                        enterOverQuotaPausedState()
                    }

                    // A blocked (taken-down) node can never play; checked on the first error so
                    // the retry loop is not spent on a request that always fails.
                    playbackRetryCount == 0 && isCurrentNodeBlocked() ->
                        enterPlaybackErrorState(AudioPlayerError.NodeBlocked)

                    playbackRetryCount < MAX_PLAYBACK_RETRIES -> {
                        playbackRetryCount++
                        // Immediate retries would burn the whole budget inside a single
                        // transient network blip; back off a little longer on each attempt.
                        delay(PLAYBACK_RETRY_BACKOFF * playbackRetryCount)
                        // The over-quota event can land during the backoff; a prepare() now
                        // would re-request the stream and re-raise the warning, so bail out.
                        if (isTransferOverQuota) {
                            enterOverQuotaPausedState()
                        } else {
                            player?.prepare()
                        }
                    }

                    else -> enterPlaybackErrorState(
                        AudioPlayerError.RetryExhausted(
                            isNetworkAvailable = runCatching { isConnectedToInternetUseCase() }
                                .getOrDefault(true),
                        )
                    )
                }
            }
        }
    }

    private suspend fun isCurrentNodeBlocked(): Boolean {
        val handle = player?.currentMediaItem?.mediaId
            ?.let { mediaHandleStore.getHandle(it) } ?: return false
        return runCatching { checkNodeAccessibilityUseCase(NodeId(handle)) }
            .exceptionOrNull() is BlockedMegaException
    }

    /**
     * Terminal playback error: retries cannot help, so playback is stopped (like the over-quota
     * state, keeping the queue and position so a deliberate play press can retry with a fresh
     * budget — see onPlayWhenReadyChanged), the mini player is hidden, and the error is parked
     * for the player screen to announce.
     */
    private fun enterPlaybackErrorState(error: AudioPlayerError) {
        player?.pause()
        player?.stop()
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false)
        audioPlayerErrorEventQueue.emit(error)
    }

    override fun onUpdateNotification(session: MediaSession, startInForeground: Boolean) {
        if (isNotificationDismissed && !session.player.isPlaying) return
        if (session.player.isPlaying) isNotificationDismissed = false
        super.onUpdateNotification(session, startInForeground)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        if (isTrustedController(controllerInfo)) mediaSession else null

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

            ACTION_PAUSE -> {
                // playWhenReady, not isPlaying: a track still buffering would otherwise dodge
                // the pause and start sounding mid-call once ready. Only intended playback
                // earns an automatic resume — pausing a player the user already paused must
                // not override their choice later.
                if (player?.playWhenReady == true) {
                    pausedByExternalCommand = true
                    player?.pause()
                }
                return START_NOT_STICKY
            }

            ACTION_RESUME -> {
                if (pausedByExternalCommand) {
                    pausedByExternalCommand = false
                    player?.play()
                }
                return START_NOT_STICKY
            }
        }
        super.onStartCommand(intent, flags, startId)
        // A null intent is the system re-creating a sticky service after its process died. The
        // queue and position live only in memory, so the restarted service has nothing to play:
        // stop it instead of leaving an idle player (and its activation analytics) behind.
        val safeIntent = intent ?: run {
            stopUnstartedService(startId)
            return START_NOT_STICKY
        }
        // stopPlayerAndSelf() releases the session before onDestroy() runs; a start intent
        // landing in that teardown window would drive playback on a service with no session
        // (onGetSession returns null, no controller can connect, no notification) and leave the
        // startForegroundService contract unmet -> FGS timeout crash. Reject it; the service is
        // about to die and a fresh launch will recreate it.
        if (mediaSession == null) {
            stopUnstartedService(startId)
            return START_NOT_STICKY
        }
        // Every start intent arrives through startForegroundService() and must be answered by
        // startForeground(), which Media3 only issues once playback actually starts. An intent
        // that does not start anything on a player with nothing loaded can never meet that
        // contract, so discharge it explicitly (see stopUnstartedService).
        if (!safeIntent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            if (player?.mediaItemCount == 0) stopUnstartedService(startId)
            return START_NOT_STICKY
        }
        val params = AudioPlayQueueParams.from(safeIntent) ?: run {
            if (player?.mediaItemCount == 0) stopUnstartedService(startId)
            return START_NOT_STICKY
        }
        // Accumulate the needStopHttpServer flag from intents across play queue rebuilds.
        needStopStreamingServer = needStopStreamingServer || params.needStopHttpServer
        // Relaunching into the queue the player already holds must not rebuild it: a rebuild
        // regenerates every media ID and resets the player, audibly interrupting playback of
        // the very track the user just tapped.
        if (lastPlayQueueParams?.isSameQueue(params) == true &&
            absorbSamePlaylistLaunch(params.handle)
        ) {
            lastPlayQueueParams = params
            MiniAudioPlayerController.notifyV2AudioPlayerPlaying(true)
            return START_NOT_STICKY
        }
        lastPlayQueueParams = params
        rebuildPlayQueue(params)
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(true)
        trackPlaybackInfoJob?.cancel()
        trackPlaybackInfoJob = lifecycleScope.launch {
            player?.let { exoPlayer ->
                runCatching {
                    trackAndSaveAudioPlaybackInfoUseCase {
                        buildCurrentPlaybackInfo(exoPlayer).also(::cachePlaybackInfo)
                    }
                }.onFailure {
                    if (it is CancellationException) throw it
                    Timber.e(it, "Audio playback tracking stopped unexpectedly")
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildCurrentPlaybackInfo(exoPlayer: ExoPlayer): MediaPlaybackInfo {
        val handle = exoPlayer.currentMediaItem?.mediaId
            ?.let { mediaHandleStore.getHandle(it) } ?: -1L
        return MediaPlaybackInfo(
            mediaHandle = handle,
            totalDuration = exoPlayer.duration.coerceAtLeast(0L),
            currentPosition = exoPlayer.currentPosition,
            mediaType = MediaType.Audio,
        )
    }

    private fun cachePlaybackInfo(info: MediaPlaybackInfo) {
        if (info.mediaHandle == -1L) return
        lastPlaybackInfo = info
        playedPastThresholdTracker.track(info)
    }

    private fun snapshotAndCachePlaybackInfo(): MediaPlaybackInfo? =
        player?.let(::buildCurrentPlaybackInfo)?.also(::cachePlaybackInfo)

    /**
     * Persists the player's current position immediately on session-ending events (pause, stop,
     * seek, track change, app backgrounding), so the periodic save interval only bounds the
     * position loss on a hard kill or crash.
     */
    private fun forceSaveCurrentPlaybackInfo() {
        snapshotAndCachePlaybackInfo()?.let(::persistPlaybackInfo)
    }

    private fun persistPlaybackInfo(info: MediaPlaybackInfo) {
        if (playedPastThresholdTracker.isStartOfUnplayedItem(info)) return
        applicationScope.launch {
            runCatching { persistAudioPlaybackInfoUseCase(info) }
                .onFailure { Timber.e(it, "Failed to save audio playback info") }
        }
    }

    /**
     * On track change the final position of the item being left is persisted. Its duration
     * comes from the last tracked snapshot, because the player already reports the next item's.
     */
    private fun saveLeftItemPlaybackInfo(oldPosition: Player.PositionInfo) {
        val oldHandle = oldPosition.mediaItem?.mediaId
            ?.let { mediaHandleStore.getHandle(it) } ?: return
        val last = lastPlaybackInfo?.takeIf { it.mediaHandle == oldHandle } ?: return
        lastPlaybackInfo = null
        persistPlaybackInfo(last.copy(currentPosition = oldPosition.positionMs))
    }

    /**
     * Handles a launch into the already-loaded queue without rebuilding it: the same track
     * just keeps playing, another track is sought within the existing timeline (resuming from
     * its saved position like a rebuild would, announcement included).
     *
     * @return true when the launch was absorbed; false when a full rebuild is still required
     */
    private fun absorbSamePlaylistLaunch(handle: Long): Boolean {
        val currentPlayer = player ?: return false
        if (currentPlayer.playbackState == Player.STATE_IDLE || currentPlayer.mediaItemCount == 0) {
            return false
        }
        val currentHandle = currentPlayer.currentMediaItem?.mediaId
            ?.let { mediaHandleStore.getHandle(it) }
        if (currentHandle == handle) {
            // A track that played to its end sits in STATE_ENDED with playWhenReady still true,
            // so a bare play() is a no-op — Media3 needs a seek to leave ENDED. Without this the
            // relaunch neither plays nor fires a player event, so startForegroundService goes
            // unsatisfied and the service is killed for not starting in time.
            if (currentPlayer.playbackState == Player.STATE_ENDED) {
                currentPlayer.seekToDefaultPosition()
            }
            currentPlayer.play()
            return true
        }
        val targetIndex = (0 until currentPlayer.mediaItemCount).firstOrNull { index ->
            mediaHandleStore.getHandle(currentPlayer.getMediaItemAt(index).mediaId) == handle
        } ?: return false
        // Cancel a still-running absorb before relaunching: two quick same-playlist taps would
        // otherwise leave two coroutines racing the resume-position seek, and the stale one could
        // land last.
        absorbJob?.cancel()
        absorbJob = lifecycleScope.launch {
            // Switching items invalidates an announcement the player screen never showed,
            // exactly like the rebuild path does.
            audioResumeEventQueue.consume()
            val resumePositionMs = runCatching { getAudioResumePositionUseCase(handle) }
                .onFailure {
                    if (it is CancellationException) throw it
                    Timber.e(it, "Failed to resolve audio resume position")
                }
                .getOrNull()
            if (resumePositionMs != null) {
                currentPlayer.seekTo(targetIndex, resumePositionMs)
                audioResumeEventQueue.emit(AudioResumeEvent(handle, resumePositionMs))
            } else {
                currentPlayer.seekToDefaultPosition(targetIndex)
            }
            currentPlayer.play()
        }
        return true
    }

    private fun rebuildPlayQueue(params: AudioPlayQueueParams) {
        playQueueBuildJob?.cancel()
        // A new session gets a fresh retry budget, and an in-flight backoff from the previous
        // session must not fire a stray prepare() into it.
        retryJob?.cancel()
        // A pending same-playlist absorb resolves a resume position then seeks; against the new
        // timeline that seek could target a stale index (IllegalSeekPositionException).
        absorbJob?.cancel()
        playbackRetryCount = 0
        // A fresh launch is a deliberate play request, like pressing play while over quota
        // (handlePlayRequestedWhileOverQuota): the stream is requested again and the SDK raises
        // the warning again if the quota window is still open. Skipping prepare() here instead
        // would leave the just-started service idle and in the background, so the
        // startForegroundService() contract behind this launch would never be met.
        isTransferOverQuota = false
        playQueueBuildJob = lifecycleScope.launch {
            // Starting new playback invalidates any terminal error the screen never showed.
            audioPlayerErrorEventQueue.clear()
            val isOpeningDifferentItem = player?.currentMediaItem?.mediaId
                ?.let { mediaHandleStore.getHandle(it) } != params.handle
            // Relaunching the track already playing (e.g. tapping it again from a different
            // source, so isSameQueue misses and a full rebuild runs) must keep its live
            // position: the persisted position is null for tracks under the resume threshold,
            // which would otherwise restart a mid-play track from zero. Snapshot before the
            // suspending query so a track change during it cannot skew the value.
            val livePositionMs = if (!isOpeningDifferentItem &&
                player?.playbackState != Player.STATE_ENDED
            ) {
                player?.currentPosition?.takeIf { it > 0 }
            } else {
                // A finished track sits at its duration; carrying that into the rebuild would
                // start it at the very end and immediately finish. Fall back to the persisted
                // position (or 0) so it restarts instead.
                null
            }
            // Switching items invalidates an announcement the player screen never showed —
            // the queue outlives the screen's ViewModel, and a leftover event would otherwise
            // be announced against this item and let Restart discard its saved position.
            if (isOpeningDifferentItem) audioResumeEventQueue.consume()
            val resumePositionMs = livePositionMs
                ?: runCatching { getAudioResumePositionUseCase(params.handle) }
                    .onFailure {
                        if (it is CancellationException) throw it
                        Timber.e(it, "Failed to resolve audio resume position")
                    }
                    .getOrNull()
            shouldAnnounceResume = isOpeningDifferentItem && resumePositionMs != null
            var anySourcesApplied = false
            audioPlayQueueBuilder(params)
                .catch { Timber.e(it, "Failed to build audio play queue") }
                .collect { mediaSources ->
                    needStopStreamingServer =
                        needStopStreamingServer || mediaSources.serverStarted
                    applyMediaSources(mediaSources, resumePositionMs)
                    anySourcesApplied = anySourcesApplied || mediaSources.mediaItems.isNotEmpty()
                }
            // Nothing was produced and nothing is loaded from before: the screen would stay on
            // its loading state forever, so surface the failure instead.
            if (!anySourcesApplied && player?.mediaItemCount == 0) {
                audioPlayerErrorEventQueue.emit(
                    AudioPlayerError.QueueBuildFailed(
                        isNetworkAvailable = runCatching { isConnectedToInternetUseCase() }
                            .getOrDefault(true),
                    )
                )
                // Nothing will ever start on this player, so Media3 never calls startForeground()
                // and the startForegroundService() behind this launch would time out. The screen
                // has no active session to lose, so it keeps showing the error above.
                startPlaceholderForeground()
                stopPlayerAndSelf()
            }
        }
    }

    /**
     * Rebuilds the play queue after the hidden-nodes filter inputs change.
     *
     * Unlike [rebuildPlayQueue] this is anchored on the item that is currently playing rather
     * than the originally launched one, skips the fast single-item emission, and applies the
     * result as an in-place diff so playback is not interrupted or repositioned.
     */
    private fun rebuildPlayQueueForFilterChange(params: AudioPlayQueueParams) {
        // A single-track session has no sibling queue to filter. Rebuilding anyway would fall
        // back to the root folder, because a no-queue launch intent carries no parent handle.
        if (!params.isPlayQueue) return
        playQueueBuildJob?.cancel()
        retryJob?.cancel()
        absorbJob?.cancel()
        playbackRetryCount = 0
        playQueueBuildJob = lifecycleScope.launch {
            val currentHandle = player?.currentMediaItem?.mediaId
                ?.let { mediaHandleStore.getHandle(it) }
            val anchoredParams = currentHandle?.let { params.copy(handle = it) } ?: params
            audioPlayQueueBuilder.buildFullQueue(anchoredParams)
                .catch { Timber.e(it, "Failed to rebuild audio play queue after filter change") }
                .collect { mediaSources ->
                    needStopStreamingServer =
                        needStopStreamingServer || mediaSources.serverStarted
                    applyFilterRebuild(mediaSources)
                }
        }
    }

    /**
     * Applies a filter-change rebuild as a diff around the currently playing item.
     *
     * The branch selection lives in [AudioPlayQueueFilterDiff]: keep the playing item's
     * MediaItem instance and rebuild around it (playback and position untouched), reset onto
     * the nearest surviving successor preserving playWhenReady, or — when nothing survived the
     * filter — stop the player entirely, since keeping hidden content playing would defeat the
     * privacy setting.
     */
    private fun applyFilterRebuild(mediaSources: MediaPlaySources) {
        val currentPlayer = player ?: return
        val newItems = mediaSources.mediaItems

        val beforeIds = timelineMediaIds(currentPlayer)
        val currentIndex = currentPlayer.currentMediaItemIndex
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = List(currentPlayer.mediaItemCount) { index ->
                mediaHandleStore.getHandle(currentPlayer.getMediaItemAt(index).mediaId)
            },
            oldCurrentIndex = currentIndex,
            newHandles = newItems.map { mediaHandleStore.getHandle(it.mediaId) },
        )

        when (action) {
            AudioPlayQueueFilterDiff.Action.StopPlayback -> {
                stopPlayerAndSelf()
                return
            }

            is AudioPlayQueueFilterDiff.Action.KeepCurrent -> {
                val newIndexOfCurrent = action.newIndexOfCurrent
                if (currentIndex + 1 < currentPlayer.mediaItemCount) {
                    currentPlayer.removeMediaItems(currentIndex + 1, currentPlayer.mediaItemCount)
                }
                if (currentIndex > 0) {
                    currentPlayer.removeMediaItems(0, currentIndex)
                }
                if (newIndexOfCurrent > 0) {
                    currentPlayer.addMediaItems(0, newItems.subList(0, newIndexOfCurrent))
                }
                if (newIndexOfCurrent + 1 < newItems.size) {
                    currentPlayer.addMediaItems(
                        newIndexOfCurrent + 1,
                        newItems.subList(newIndexOfCurrent + 1, newItems.size),
                    )
                }
                // The rebuilt duplicate of the playing item never enters the timeline (the old
                // instance is kept), so its freshly registered mapping must be dropped explicitly.
                mediaHandleStore.remove(listOf(newItems[newIndexOfCurrent].mediaId))
            }

            is AudioPlayQueueFilterDiff.Action.ResetToSurvivor -> {
                currentPlayer.setMediaItems(newItems, action.startIndex, 0L)
                currentPlayer.prepare()
            }
        }
        mediaHandleStore.remove(beforeIds - timelineMediaIds(currentPlayer))
    }

    private fun timelineMediaIds(currentPlayer: Player): Set<String> = buildSet {
        for (i in 0 until currentPlayer.mediaItemCount) {
            add(currentPlayer.getMediaItemAt(i).mediaId)
        }
    }

    private fun applyMediaSources(mediaSources: MediaPlaySources, resumePositionMs: Long?) {
        val currentPlayer = player ?: return
        if (mediaSources.mediaItems.isEmpty()) return

        val beforeIds = timelineMediaIds(currentPlayer)
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
            // The start position is applied on every emission that reaches this branch, and that
            // is deliberate. The queue builder emits twice -- a single item for a fast start, then
            // the full queue -- and the second emission normally takes the addMediaItems branch
            // above, so the position is set once. When it does reach setMediaItems again the
            // player has been reset (STATE_IDLE), and starting from zero there would lose the
            // resume the user was just told about. A rebuild around the item already playing
            // keeps the position for the same reason but is not announced: the user has seen
            // that resume already, and re-announcing it on every rebuild would be noise.
            currentPlayer.setMediaItems(
                mediaSources.mediaItems,
                startIndex,
                resumePositionMs ?: 0L,
            )
            // Over quota: keep the queue set but do not re-request the stream. An in-flight
            // full-queue emission reaches this branch after the over-quota stop (which leaves
            // the player IDLE), and prepare()/play() here would grab the stream again and
            // re-raise the warning the stop just handled.
            if (!isTransferOverQuota) {
                currentPlayer.prepare()
                currentPlayer.play()
                if (shouldAnnounceResume && resumePositionMs != null) {
                    shouldAnnounceResume = false
                    targetMediaId?.let { mediaHandleStore.getHandle(it) }?.let { handle ->
                        audioResumeEventQueue.emit(AudioResumeEvent(handle, resumePositionMs))
                    }
                }
            }
        }
        // Drop handle mappings only for the IDs this update removed from the timeline; entries
        // registered by a queue build still in flight must stay untouched.
        mediaHandleStore.remove(beforeIds - timelineMediaIds(currentPlayer))
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
                forceSaveCurrentPlaybackInfo()
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
        // Cancel in-flight work before releasing: onDestroy() is deferred until bound clients
        // unbind, and a queue build / retry backoff / absorb seek completing in that window
        // would re-start playback on a service the user just stopped.
        playQueueBuildJob?.cancel()
        retryJob?.cancel()
        absorbJob?.cancel()
        forceSaveCurrentPlaybackInfo()
        MiniAudioPlayerController.notifyV2AudioPlayerPlaying(false)
        // Stop audio immediately so playback cannot continue even if the service is kept alive
        // by an external MediaController binding (e.g. a notification-listener app that obtained
        // a MediaController via MediaSessionManager.getActiveSessions()).  Without this, stopSelf()
        // is deferred until the external client unbinds, and onDestroy() never runs in the
        // meantime, leaving ExoPlayer playing in the background.
        runCatching { player?.stop() }
            .onFailure { Timber.e(it, "Failed to stop ExoPlayer") }
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

    /**
     * Stops a service that was started with startForegroundService() but will never start
     * playback. Stopping alone is not enough: the system treats a service that is brought down
     * while still owing its startForeground() as a contract violation and crashes the process
     * with ForegroundServiceDidNotStartInTimeException, exactly as if it had timed out. Entering
     * the foreground with a placeholder notification first settles the request; the notification
     * is removed again in the same call.
     */
    private fun stopUnstartedService(startId: Int) {
        startPlaceholderForeground()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    /**
     * Answers a pending startForegroundService() request on a player that Media3 will not promote
     * itself (nothing is going to play). Uses the audio player channel with a bare notification;
     * callers stop the service, or remove the notification, straight after.
     */
    private fun startPlaceholderForeground() {
        runCatching {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID,
                    getString(R.string.audio_player_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID)
                .setSmallIcon(R.drawable.ic_mega_logo)
                .build()
            ServiceCompat.startForeground(
                this,
                PLACEHOLDER_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                },
            )
        }.onFailure { Timber.e(it, "Failed to enter the foreground for an unstarted audio service") }
    }

    override fun onDestroy() {
        isRunning = false
        forceSaveCurrentPlaybackInfo()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        playQueueBuildJob?.cancel()
        trackPlaybackInfoJob?.cancel()
        if (needStopStreamingServer) {
            // applicationScope, not lifecycleScope: super.onDestroy() cancels the lifecycle
            // scope before the first suspending stop call can resume, so the second stop —
            // the folder-link server — would never execute and the server would leak for the
            // rest of the process lifetime.
            applicationScope.launch {
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
        mediaHandleStore.clear()
        super.onDestroy()
    }

    companion object {
        /**
         * Whether an instance of [AudioPlayerService] is currently running.
         * Set in [onCreate] and cleared in [onDestroy].
         */
        @Volatile
        var isRunning = false
            @VisibleForTesting internal set

        private const val MAX_PLAYBACK_RETRIES = 6

        /** Distinct from Media3's own notification id so the two never overwrite each other. */
        private const val PLACEHOLDER_NOTIFICATION_ID = 1002
        private val PLAYBACK_RETRY_BACKOFF = 500.milliseconds

        private const val HTTP_TIMEOUT_MS = 5_000
        private const val LOAD_ERROR_RETRY_COUNT = 2

        // ExoPlayer buffer configuration for low-RAM devices (heap growth limit ≤ 128 MB);
        // values aligned with the legacy MediaPlayerFacade.
        private const val LOW_RAM_TARGET_BUFFER_BYTES = 12 * 1024 * 1024 // 12 MB
        private const val LOW_RAM_MIN_BUFFER_MS = 15_000
        private const val LOW_RAM_MAX_BUFFER_MS = 30_000
        private const val LOW_RAM_BUFFER_FOR_PLAYBACK_MS = 2_500
        private const val LOW_RAM_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

        private const val ACTION_STOP =
            "mega.privacy.android.app.mediaplayer.AudioPlayerService.STOP"
        private const val ACTION_DISMISS =
            "mega.privacy.android.app.mediaplayer.AudioPlayerService.DISMISS"
        private const val ACTION_PAUSE =
            "mega.privacy.android.app.mediaplayer.AudioPlayerService.PAUSE"
        private const val ACTION_RESUME =
            "mega.privacy.android.app.mediaplayer.AudioPlayerService.RESUME"

        /**
         * Returns true if [callerPackage] should be allowed to connect to the MediaSession.
         *
         * Only MEGA itself ([appPackage]) and the Android system process are permitted.
         * The system process routes media button events from Bluetooth devices and the lock
         * screen. All other callers are rejected to prevent third-party apps from reading
         * node handles out of the Media3 session timeline.
         */
        internal fun isPackageTrusted(callerPackage: String, appPackage: String): Boolean =
            callerPackage == appPackage || callerPackage == "android"

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
        /**
         * Starts the service, marking it as running before the request is handed to the system:
         * [onCreate] only executes on a later main-loop pass, and a stop issued inside that
         * window must still reach the service instead of being dropped by [stopAudioPlayer]'s
         * guard.
         */
        fun start(context: Context, intent: Intent) {
            val wasRunning = isRunning
            isRunning = true
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Nothing was started: leave the flag as it was, or every later stop / pause /
                // resume command would be sent to a service that does not exist.
                isRunning = wasRunning
                throw e
            }
        }

        fun stopAudioPlayer(context: Context) {
            // Delivered via startService like the other commands: the isRunning guard inside
            // sendCommand keeps it from starting a service that is not running just to stop it
            // (spurious onCreate, analytics event, player construction).
            sendCommand(context, ACTION_STOP)
        }

        /**
         * Pauses playback for an external interruption (a call starting, video playback).
         * The pause is remembered so [resumeAudioPlayer] can undo it; a player the user had
         * already paused is left alone.
         */
        fun pauseAudioPlayer(context: Context) {
            sendCommand(context, ACTION_PAUSE)
        }

        /**
         * Resumes playback previously paused by [pauseAudioPlayer]. A pause the user made
         * themselves is not overridden.
         */
        fun resumeAudioPlayer(context: Context) {
            sendCommand(context, ACTION_RESUME)
        }

        private fun sendCommand(context: Context, action: String) {
            if (!isRunning) return
            // startService can throw when the app is backgrounded without an active foreground
            // service; a lost command for a player that is not audibly playing is harmless.
            runCatching {
                context.startService(
                    Intent(context, AudioPlayerService::class.java).apply {
                        this.action = action
                    }
                )
            }.onFailure { Timber.w(it, "Failed to deliver $action to AudioPlayerService") }
        }
    }
}
