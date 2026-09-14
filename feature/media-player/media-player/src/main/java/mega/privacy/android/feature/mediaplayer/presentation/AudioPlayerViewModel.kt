package mega.privacy.android.feature.mediaplayer.presentation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.DeleteAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerError
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerErrorEventQueue
import mega.privacy.android.feature.mediaplayer.data.AudioResumeEventQueue
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.feature.mediaplayer.data.model.AudioControllerState
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import mega.privacy.android.feature.mediaplayer.presentation.model.SleepTimerOption
import mega.privacy.android.feature.mediaplayer.presentation.model.SleepTimerState
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_LINK_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.mobile.analytics.event.AudioPlayerLoopPlayingItemEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerLoopQueueEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerShuffleEnabledEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * ViewModel for the revamped audio player.
 *
 * Collects raw player state from [AudioMediaControllerGateway] and maps it to [uiState].
 * Shuffle changes are persisted and analytics events are tracked here, keeping the gateway
 * focused on Media3 interaction only. Repeat mode is written straight to the preference by
 * [cycleRepeatMode] instead, because the player service derives it from there.
 *
 * [isPodcastMode] is resolved as early as possible: when [startPlayback] receives the launch
 * intent, the node's duration is fetched from [GetNodeByHandleUseCase] before Media3 connects,
 * so the correct mode is shown even during the initial [AudioPlayerUiState.Loading] phase.
 * Once Media3 reports the actual duration the value is confirmed (or corrected). The user may
 * override the auto-detected mode at any time via [togglePlayerMode]; the override resets when
 * the playing item changes.
 */
@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    private val gateway: AudioMediaControllerGateway,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val setAudioShuffleEnabledUseCase: SetAudioShuffleEnabledUseCase,
    private val setAudioRepeatModeUseCase: SetAudioRepeatModeUseCase,
    private val deleteAudioPlaybackInfoUseCase: DeleteAudioPlaybackInfoUseCase,
    private val repeatToggleModeByExoPlayerMapper: RepeatToggleModeByExoPlayerMapper,
    private val audioResumeEventQueue: AudioResumeEventQueue,
    private val audioPlayerErrorEventQueue: AudioPlayerErrorEventQueue,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val playerState = MutableStateFlow<AudioPlayerUiState>(AudioPlayerUiState.Loading)

    val uiState: StateFlow<AudioPlayerUiState> =
        playerState.asUiStateFlow(viewModelScope, AudioPlayerUiState.Loading)

    // Defaults to music: most audio is shorter than the podcast threshold, so this is the
    // least likely wrong guess for sources the prefetch cannot resolve (chat, file links, zip).
    private val _isPodcastMode = MutableStateFlow(false)

    /** Whether the player is currently in podcast mode. Available from the moment the launch
     * intent is processed, before Media3 finishes connecting. */
    val isPodcastMode: StateFlow<Boolean> = _isPodcastMode.asStateFlow()

    private val _resumedFromPosition = MutableStateFlow<String?>(null)

    /**
     * Node handle of the most recent resume announcement, read by [restartFromBeginning]. It is
     * deliberately not cleared by [onResumeEventConsumed]: the screen consumes the announcement as
     * soon as the snackbar is queued, while the Restart tap arrives seconds later.
     */
    private var resumedFromHandle: Long? = null

    /** Formatted position a silent resume started from, set while the resume snackbar is still
     * to be shown and cleared via [onResumeEventConsumed]. */
    val resumedFromPosition: StateFlow<String?> = _resumedFromPosition.asStateFlow()

    private val _sleepTimerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)

    /** Current sleep timer state. Updated every second while a countdown is running. */
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _playbackError = MutableStateFlow<AudioPlayerError?>(null)

    /** Terminal playback error waiting to be announced, cleared via [onPlaybackErrorConsumed]. */
    val playbackError: StateFlow<AudioPlayerError?> = _playbackError.asStateFlow()

    /**
     * Emits true once the connected player holds a non-empty queue. Lets the route entry decide
     * whether a screen restored without a launch intent (process death) still has a running
     * session to show, or should navigate back.
     */
    val hasActiveSession: Flow<Boolean> = gateway.playerState.map { it.mediaItemCount > 0 }

    /**
     * True once a launch intent has been handed to [startPlayback] in this process. The route
     * entry uses it to tell a configuration-change re-entry (payload already consumed, playback
     * possibly still loading) apart from a restore after process death.
     */
    var hasStartedPlayback: Boolean = false
        private set

    private val _sessionEnded = MutableStateFlow(false)

    /**
     * True when the playback session this screen was showing has ended (the service stopped:
     * queue emptied by the hidden-nodes filter, mini player closed, logout). The route entry
     * navigates back so the screen does not keep displaying content that no longer plays --
     * or, for a hidden queue, content the user just asked to hide. Cleared via
     * [onSessionEndedConsumed] and when new playback starts.
     */
    val sessionEnded: StateFlow<Boolean> = _sessionEnded.asStateFlow()

    private var hadActiveSession = false

    private val _controlBlockedByCall = MutableStateFlow<StateEvent>(consumed)

    /**
     * Triggered when a playback control was refused because a MEGA call is ongoing; the screen
     * announces it with a snackbar and consumes it via [onControlBlockedByCallConsumed].
     */
    val controlBlockedByCall: StateFlow<StateEvent> = _controlBlockedByCall.asStateFlow()

    private var currentControllerState: AudioControllerState? = null
    private var lastMediaItemId: String? = null
    private var userOverriddenMode: Boolean? = null
    private var prefetchJob: Job? = null
    private var nodeNameJob: Job? = null
    private var nodeNameHandle: Long? = null
    private var sleepTimerJob: Job? = null
    private var endOfTrackSourceItemId: String? = null

    // The repeat mode the last tap asked for, held until the player reports it. Taps arrive
    // faster than the write can travel through the preference and back, so cycling from what
    // the player currently reports would derive the same next mode twice and lose a tap.
    // Reconciled against any reported mode once no writes are in flight (see handleSideEffects):
    // another writer (e.g. the queue screen's continuous-playback toggle) may have settled the
    // player on a mode this value never asked for, and it must not go stale.
    private var pendingRepeatMode: RepeatToggleMode? = null
    private var pendingRepeatModeWrites = 0

    private data class IntentData(
        val adapterType: Int,
        val nodeSourceType: NodeSourceType,
        val fileLinkUrl: String?,
        val localFilePath: String?,
        val chatId: Long?,
        val msgId: Long?,
    )

    private var intentData: IntentData? = null

    init {
        observePlayerState()
        observeResumeEvents()
        observePlaybackErrors()
        observeNodeUpdates()
    }

    /**
     * Narrows the global node-update stream to the item currently playing: a rename refreshes
     * the displayed name, a removal moves playback off the deleted node so the player does not
     * keep serving a node that no longer exists.
     */
    private fun observeNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase()
                .catch { Timber.e(it, "Failed to monitor node updates") }
                .collect { update ->
                    val currentHandle =
                        (playerState.value as? AudioPlayerUiState.Data)?.currentPlayingHandle
                            ?: return@collect
                    val changes = update.changes.entries
                        .firstOrNull { it.key.id.longValue == currentHandle }
                        ?.value ?: return@collect
                    if (NodeChanges.Name in changes) {
                        fetchNodeName(currentHandle)
                    }
                    if (NodeChanges.Remove in changes) {
                        skipPastRemovedNode()
                    }
                }
        }
    }

    /**
     * At the queue boundary skipToNext is a no-op and the player would keep serving the removed
     * node, so stop playback instead when no other item can take over. The item count is checked
     * as well: under repeat-all a single item reports [AudioControllerState.hasNext], but the only
     * "next" would be the removed node itself.
     */
    private fun skipPastRemovedNode() {
        val state = currentControllerState
        val hasOtherItem = state != null && state.mediaItemCount > 1 && state.hasNext
        if (hasOtherItem) gateway.skipToNext() else gateway.stop()
    }

    private fun observePlaybackErrors() {
        viewModelScope.launch {
            audioPlayerErrorEventQueue.events.collect {
                // The collected value is only a wake-up signal; consume() claims the event
                // atomically so a concurrent clear() cannot lead to a double delivery.
                audioPlayerErrorEventQueue.consume()?.let { error ->
                    _playbackError.value = error
                }
            }
        }
    }

    /** Marks the pending playback error as shown so the dialog is not repeated. */
    fun onPlaybackErrorConsumed() {
        _playbackError.value = null
    }

    /** Marks the session-ended signal as handled so it fires at most once per session. */
    fun onSessionEndedConsumed() {
        _sessionEnded.value = false
    }

    /** Marks the blocked-by-call announcement as shown. */
    fun onControlBlockedByCallConsumed() {
        _controlBlockedByCall.value = consumed
    }

    /**
     * Runs [action] unless a MEGA call is ongoing: starting or steering playback would grab
     * audio focus from the call, so the attempt is refused and announced instead. Pausing is
     * never routed through here — silencing the player during a call is always allowed.
     */
    private fun runUnlessInCall(action: () -> Unit) {
        viewModelScope.launch {
            val inCall = runCatching { isParticipatingInChatCallUseCase() }
                .onFailure { Timber.e(it, "Failed to check ongoing call state") }
                .getOrDefault(false)
            if (inCall) {
                _controlBlockedByCall.value = triggered
            } else {
                action()
            }
        }
    }

    private fun observeResumeEvents() {
        viewModelScope.launch {
            combine(audioResumeEventQueue.events, playerState) { event, state ->
                event to (state as? AudioPlayerUiState.Data)?.currentPlayingHandle
            }.collect { (event, currentPlayingHandle) ->
                // A track change that does not rebuild the play queue -- an auto-advance to the
                // next item -- leaves the service-side invalidation untouched, so the event must
                // be matched against what is actually playing before it is announced. The event
                // is left pending rather than dropped on a mismatch: the service emits at play(),
                // which can land before Media3 reports the new item, so a mismatch is not yet
                // proof the event is stale.
                if (event.mediaHandle != currentPlayingHandle) return@collect
                // Announce what was actually claimed rather than the combined value: a newer
                // event queued while combine still held the previous one would otherwise be
                // taken here and the older position announced in its place.
                val claimed = audioResumeEventQueue.consume() ?: return@collect
                if (claimed.mediaHandle != currentPlayingHandle) {
                    // Put it back rather than dropping it, for the same reason the check above
                    // leaves a mismatch pending: the player may simply not have reported this
                    // event's item yet. Claiming it here without re-queueing would lose it for
                    // good, since the queue no longer holds anything to re-emit.
                    audioResumeEventQueue.emit(claimed)
                    return@collect
                }
                resumedFromHandle = claimed.mediaHandle
                _resumedFromPosition.value =
                    durationInSecondsTextMapper(claimed.resumePositionMs.milliseconds)
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            var prevState: AudioControllerState? = null
            gateway.playerState
                .catch { Timber.e(it, "Failed to collect player state from gateway") }
                .collect { state ->
                    handleSideEffects(prevState, state)
                    prevState = state
                    currentControllerState = state
                    mapToUiState(state)
                }
        }
    }

    private fun handleSideEffects(prev: AudioControllerState?, current: AudioControllerState) {
        if (prev == null) {
            // Fetch node name on first emission so currentPlayingItemName serves as the filename
            // fallback while Media3 extracts the embedded title from the file's tags.
            current.currentMediaItemHandle?.let { fetchNodeName(it) }
            return
        }

        if (prev.shuffleEnabled != current.shuffleEnabled) {
            if (current.shuffleEnabled) {
                Analytics.tracker.trackEvent(AudioPlayerShuffleEnabledEvent)
            }
            viewModelScope.launch {
                runCatching { setAudioShuffleEnabledUseCase(current.shuffleEnabled) }
                    .onFailure { Timber.e(it, "Failed to persist shuffle preference") }
            }
        }

        if (prev.repeatMode != current.repeatMode) {
            val toggleMode = repeatToggleModeByExoPlayerMapper(current.repeatMode)
            // A pending mode settles on its own report; with no write left in flight, whatever
            // is reported is the final word from the last writer (which may be another screen),
            // so a stale pending value is dropped rather than kept to mis-base the next tap.
            if (toggleMode == pendingRepeatMode || pendingRepeatModeWrites == 0) {
                pendingRepeatMode = null
            }
            when (toggleMode) {
                RepeatToggleMode.REPEAT_ONE ->
                    Analytics.tracker.trackEvent(AudioPlayerLoopPlayingItemEnabledEvent)

                RepeatToggleMode.REPEAT_ALL ->
                    Analytics.tracker.trackEvent(AudioPlayerLoopQueueEnabledEvent)

                else -> {}
            }
        }

        if (prev.mediaItemCount > 0 && current.isIdle) {
            cancelSleepTimer()
        }

        if (prev.currentMediaItemId != current.currentMediaItemId) {
            current.currentMediaItemHandle?.let { fetchNodeName(it) }
            if (_sleepTimerState.value is SleepTimerState.EndOfTrack &&
                current.currentMediaItemId != endOfTrackSourceItemId
            ) {
                // End-of-track only stops playback when the source track ends on its own: an
                // automatic advance is the timer firing, so pause. A user-driven change (skip,
                // queue tap, notification next -> all reported as non-AUTO) means the user took
                // control, so just retire the timer and let their chosen track play.
                if (current.lastItemTransitionWasAuto) {
                    gateway.pause()
                }
                _sleepTimerState.value = SleepTimerState.Inactive
                endOfTrackSourceItemId = null
            }
        }

        if (_sleepTimerState.value is SleepTimerState.EndOfTrack &&
            prev.isPlaying && !current.isPlaying &&
            current.durationMs > 0 &&
            current.currentPositionMs >= current.durationMs - END_OF_TRACK_THRESHOLD_MS
        ) {
            _sleepTimerState.value = SleepTimerState.Inactive
            endOfTrackSourceItemId = null
        }

        // Under REPEAT_ONE the track end is a seamless loop: no item transition, isPlaying never
        // drops, only the position wraps back to the start. Without this the End-of-track timer
        // never fires and the track loops all night. The wrap is recognised by a large backwards
        // jump from near the end that lands near the start while playback keeps running; an
        // in-playback seek from that region to anywhere past the opening moments does not match.
        if (_sleepTimerState.value is SleepTimerState.EndOfTrack &&
            current.currentMediaItemId == endOfTrackSourceItemId &&
            current.repeatMode == Player.REPEAT_MODE_ONE &&
            prev.isPlaying && current.isPlaying &&
            prev.durationMs > 0 &&
            prev.currentPositionMs >= prev.durationMs - END_OF_TRACK_THRESHOLD_MS &&
            current.currentPositionMs <= END_OF_TRACK_THRESHOLD_MS &&
            prev.currentPositionMs > current.currentPositionMs + REPEAT_ONE_WRAP_THRESHOLD_MS
        ) {
            gateway.pause()
            _sleepTimerState.value = SleepTimerState.Inactive
            endOfTrackSourceItemId = null
        }
    }

    private fun mapToUiState(state: AudioControllerState) {
        if (state.isSessionEnded && hadActiveSession) {
            _sessionEnded.value = true
        }
        if (state.mediaItemCount == 0) {
            // Nothing is loaded in the player: a fresh connect before the service builds the
            // queue, or a restart after process death. Mapping this to Data would render an
            // empty player shell, so stay on the last state (initially Loading) until items
            // arrive.
            return
        }
        hadActiveSession = true
        val existing = playerState.value as? AudioPlayerUiState.Data
        val data = intentData
        if (state.currentMediaItemId != lastMediaItemId) {
            lastMediaItemId = state.currentMediaItemId
            userOverriddenMode = null
        }
        if (userOverriddenMode == null && state.durationMs > 0) {
            _isPodcastMode.value = state.durationMs > PODCAST_MODE_DURATION_MS
        }
        playerState.value = AudioPlayerUiState.Data(
            isPlaying = state.isPlaying,
            currentPosition = state.currentPositionMs,
            duration = state.durationMs,
            repeatMode = state.repeatMode,
            shuffleEnabled = state.shuffleEnabled,
            hasPlaylist = state.mediaItemCount > 1,
            hasNext = state.hasNext,
            isLoading = state.isBuffering,
            title = state.title,
            artist = state.artist,
            artworkUri = state.artworkUri,
            currentPlayingHandle = state.currentMediaItemHandle,
            thumbnailData = state.currentMediaItemHandle?.let { ThumbnailRequest.fromHandle(it) },
            currentPlayingItemName = existing?.currentPlayingItemName,
            currentAdapterType = data?.adapterType ?: INVALID_VALUE,
            nodeSourceType = data?.nodeSourceType ?: NodeSourceType.MEDIA_PLAYER_DEFAULT,
            fileLinkUrl = data?.fileLinkUrl,
            localFilePath = data?.localFilePath,
            chatId = data?.chatId,
            msgId = data?.msgId,
            currentPlaybackSpeed = state.playbackSpeed,
        )
    }

    private fun fetchNodeName(handle: Long) {
        nodeNameJob?.cancel()
        nodeNameHandle = handle
        nodeNameJob = viewModelScope.launch {
            runCatching { getNodeByHandleUseCase(handle)?.name }
                .onSuccess { name ->
                    // A fetch started for an earlier track can still complete after a quick skip;
                    // its name must not overwrite the track that is playing now.
                    if (handle != nodeNameHandle) return@onSuccess
                    playerState.update { state ->
                        if (state is AudioPlayerUiState.Data) state.copy(currentPlayingItemName = name)
                        else state
                    }
                }
                .onFailure {
                    if (it !is CancellationException) {
                        Timber.w(it, "Failed to fetch node name for handle=$handle")
                    }
                }
        }
    }

    private fun prefetchPlayerMode(handle: Long) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            if (userOverriddenMode != null) return@launch
            val node = runCatching { getNodeByHandleUseCase(handle) }
                .onFailure {
                    Timber.w(
                        it,
                        "Failed to prefetch node for player mode detection, handle=$handle"
                    )
                }
                .getOrNull() as? TypedAudioNode ?: return@launch
            if (userOverriddenMode != null) return@launch
            _isPodcastMode.value = node.duration.inWholeMilliseconds > PODCAST_MODE_DURATION_MS
        }
    }

    fun startPlayback(intent: Intent) {
        hasStartedPlayback = true
        hadActiveSession = false
        _sessionEnded.value = false
        val rebuildPlaylist = intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        // Before setCurrentIntent: that re-maps the intent extras onto whatever Data is showing,
        // and the previous session's Data must not be republished on its way out.
        if (rebuildPlaylist) resetStaleSessionState()
        setCurrentIntent(intent)
        val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_VALUE.toLong())
        if (handle != INVALID_VALUE.toLong()) {
            prefetchPlayerMode(handle)
        }
        if (rebuildPlaylist) {
            // This ViewModel outlives the player screen, so an error consumed while the screen
            // was closed would otherwise pop up over the brand-new playback session. Reopening
            // without a rebuild keeps the error: playback is still stopped by it, and the dialog
            // is what tells the user why.
            _playbackError.value = null
            // Same reason for a resume announcement latched while no screen was attached: left
            // in place it would be shown over the new session, and its Restart action would
            // reset — and delete the saved position of — the wrong track. The queue is drained
            // too: an event parked for a track that never became current would otherwise be
            // announced, with a stale position, the next time that track plays.
            _resumedFromPosition.value = null
            audioResumeEventQueue.consume()
            gateway.startService(intent)
        }
    }

    /**
     * Drops the last session's screen state when that session is over. The controller reports
     * an empty player on its first emission after a fresh service start, which [mapToUiState]
     * deliberately ignores, so without this the new screen would compose with the previous
     * track's data and enabled controls until the new queue arrives. A session that is still
     * live is left alone: the gateway re-emits its real state on reconnect.
     */
    private fun resetStaleSessionState() {
        val lastState = currentControllerState ?: return
        if (!lastState.isSessionEnded && lastState.mediaItemCount > 0) return
        currentControllerState = null
        lastMediaItemId = null
        playerState.value = AudioPlayerUiState.Loading
    }

    fun togglePlayPause() {
        val state = currentControllerState ?: return
        if (state.isPlaying) {
            gateway.pause()
        } else {
            runUnlessInCall { gateway.play() }
        }
    }

    fun seekTo(positionMs: Long) {
        runUnlessInCall { gateway.seekTo(positionMs) }
    }

    fun skipToNext() {
        runUnlessInCall { gateway.skipToNext() }
    }

    fun skipToPrevious() {
        runUnlessInCall { gateway.skipToPrevious() }
    }

    fun toggleShuffle() {
        val state = currentControllerState ?: return
        gateway.setShuffleEnabled(!state.shuffleEnabled)
    }

    fun togglePlayerMode() {
        val newMode = !_isPodcastMode.value
        userOverriddenMode = newMode
        _isPodcastMode.value = newMode
    }

    fun seekForward15() {
        val state = currentControllerState ?: return
        val target = if (state.durationMs > 0) {
            minOf(state.currentPositionMs + SEEK_15_SECONDS_MS, state.durationMs)
        } else {
            state.currentPositionMs + SEEK_15_SECONDS_MS
        }
        runUnlessInCall { gateway.seekTo(target) }
    }

    fun seekBackward15() {
        val state = currentControllerState ?: return
        runUnlessInCall { gateway.seekTo(maxOf(0L, state.currentPositionMs - SEEK_15_SECONDS_MS)) }
    }

    /**
     * Cycles the repeat mode NONE -> ALL -> ONE -> NONE.
     *
     * Only the preference is written: the player service observes it and applies the resulting
     * mode, so a single writer owns the repeat mode and its dependency on continuous playback.
     * The write travels through the preference and back before the player reports the new mode,
     * so the cycle starts from [pendingRepeatMode] while a previous tap is still in flight.
     */
    fun cycleRepeatMode() {
        val currentMode = pendingRepeatMode
            ?: currentControllerState?.let { repeatToggleModeByExoPlayerMapper(it.repeatMode) }
            ?: return
        val nextMode = when (currentMode) {
            RepeatToggleMode.REPEAT_NONE -> RepeatToggleMode.REPEAT_ALL
            RepeatToggleMode.REPEAT_ALL -> RepeatToggleMode.REPEAT_ONE
            RepeatToggleMode.REPEAT_ONE -> RepeatToggleMode.REPEAT_NONE
        }
        pendingRepeatMode = nextMode
        pendingRepeatModeWrites++
        viewModelScope.launch {
            try {
                runCatching { setAudioRepeatModeUseCase(nextMode.ordinal) }
                    .onFailure {
                        Timber.e(it, "Failed to save repeat mode preference")
                        // Leaving it set would make the next tap cycle on from a mode the player
                        // never reached.
                        if (pendingRepeatMode == nextMode) pendingRepeatMode = null
                    }
            } finally {
                pendingRepeatModeWrites--
            }
        }
    }

    private fun setCurrentIntent(intent: Intent) {
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE) {
            Timber.w("Audio player launched without a valid adapter type")
        }
        val rawChatId = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
        val rawMsgId = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)
        intentData = IntentData(
            adapterType = adapterType,
            nodeSourceType = adapterTypeToNodeSourceType(adapterType),
            fileLinkUrl = intent.getStringExtra(URL_FILE_LINK),
            localFilePath = intent.getStringExtra(URL_LOCAL_FILE_PATH),
            chatId = rawChatId.takeIf { it != INVALID_HANDLE },
            msgId = rawMsgId.takeIf { it != INVALID_HANDLE },
        )
        playerState.update { state ->
            if (state is AudioPlayerUiState.Data) {
                val data = intentData ?: return@update state
                state.copy(
                    currentAdapterType = data.adapterType,
                    nodeSourceType = data.nodeSourceType,
                    fileLinkUrl = data.fileLinkUrl,
                    localFilePath = data.localFilePath,
                    chatId = data.chatId,
                    msgId = data.msgId,
                )
            } else state
        }
    }

    private fun adapterTypeToNodeSourceType(adapterType: Int): NodeSourceType =
        when (adapterType) {
            OFFLINE_ADAPTER -> NodeSourceType.OFFLINE
            RUBBISH_BIN_ADAPTER -> NodeSourceType.RUBBISH_BIN
            FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING -> NodeSourceType.FOLDER_LINK
            FROM_CHAT -> NodeSourceType.CHAT
            FILE_LINK_ADAPTER -> NodeSourceType.FILE_LINK
            FROM_IMAGE_VIEWER -> NodeSourceType.MEDIA_PLAYER_IMAGE_VIEWER
            VERSIONS_ADAPTER -> NodeSourceType.MEDIA_PLAYER_VERSIONS
            ZIP_ADAPTER -> NodeSourceType.MEDIA_PLAYER_ZIP_FILE
            else -> NodeSourceType.MEDIA_PLAYER_DEFAULT
        }

    /**
     * Starts or replaces the sleep timer with the given [option].
     *
     * For timed options a countdown coroutine is launched that ticks every second and pauses
     * playback when it reaches zero. For [SleepTimerOption.EndOfTrack], playback pauses
     * automatically when the current track finishes. Calling this while a timer is already
     * active replaces it.
     */
    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        endOfTrackSourceItemId = null

        when (option) {
            SleepTimerOption.EndOfTrack -> {
                endOfTrackSourceItemId = currentControllerState?.currentMediaItemId
                _sleepTimerState.value = SleepTimerState.EndOfTrack
            }

            else -> {
                // Anchored on a wall-clock deadline rather than decremented per tick: each
                // delay() wakes slightly late, and over an hour those overshoots would add up
                // to the timer firing noticeably after the time the user picked.
                val deadlineMillis =
                    getCurrentTimeInMillisUseCase() + option.duration.inWholeMilliseconds
                _sleepTimerState.value = SleepTimerState.CountingDown(option.duration, option)
                sleepTimerJob = viewModelScope.launch {
                    var remaining = option.duration
                    while (remaining > Duration.ZERO) {
                        delay(minOf(SLEEP_TIMER_TICK, remaining))
                        // Upper bound guards against the wall clock jumping backwards, which
                        // would otherwise stretch the timer past the duration the user picked.
                        remaining = (deadlineMillis - getCurrentTimeInMillisUseCase())
                            .coerceIn(0L, option.duration.inWholeMilliseconds).milliseconds
                        _sleepTimerState.value = SleepTimerState.CountingDown(remaining, option)
                    }
                    gateway.pause()
                    _sleepTimerState.value = SleepTimerState.Inactive
                }
            }
        }
    }

    /**
     * Cancels any active sleep timer and resets the state to [SleepTimerState.Inactive].
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        endOfTrackSourceItemId = null
        _sleepTimerState.value = SleepTimerState.Inactive
    }

    /** Marks the pending resume event as shown so the snackbar is not repeated. */
    fun onResumeEventConsumed() {
        _resumedFromPosition.value = null
    }

    /**
     * Restarts the resumed item from the beginning, undoing a silent resume. No-op when nothing
     * has been announced yet or the resumed item is no longer the one playing.
     *
     * The saved position is discarded as well as sought past: a snapshot at the start is below
     * the minimum playback threshold and is never persisted, so seeking alone would leave the
     * old record intact and silently resume from it again if the player is closed before
     * playback passes that threshold.
     */
    fun restartFromBeginning() {
        val handle = resumedFromHandle ?: return
        // The snackbar lingers ~4s; if the resumed track has auto-advanced since, the current
        // item is no longer the one the announcement was about, so seeking it to 0 and discarding
        // its saved position would hit the wrong track. Only act while they still match.
        val currentHandle = (playerState.value as? AudioPlayerUiState.Data)?.currentPlayingHandle
        if (currentHandle != handle) return
        gateway.seekTo(0L)
        // The snackbar sits where the user dismisses the screen from, so leaving right after
        // tapping Restart is a natural gesture; on viewModelScope the delete would be cancelled
        // and the discarded position would silently resume on the next open.
        applicationScope.launch {
            runCatching { deleteAudioPlaybackInfoUseCase(handle) }
                .onFailure { Timber.e(it, "Failed to discard audio playback info on restart") }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        gateway.setPlaybackSpeed(speed)
    }

    fun stopPlayer() {
        gateway.stop()
    }

    override fun onCleared() {
        gateway.release()
        super.onCleared()
    }

    companion object {
        // Tracks longer than 12 minutes are automatically detected as podcasts; shorter tracks are classified as music.
        // The 12-minute threshold is defined in the design specification.
        private const val PODCAST_MODE_DURATION_MS = 12 * 60 * 1_000L
        private const val SEEK_15_SECONDS_MS = 15_000L
        private val SLEEP_TIMER_TICK = 1.seconds
        private const val END_OF_TRACK_THRESHOLD_MS = 1_500L

        // A repeat-one loop rewinds by nearly the whole track; anything above this is treated
        // as the wrap rather than position-poll jitter.
        private const val REPEAT_ONE_WRAP_THRESHOLD_MS = 5_000L

        // Intent extra keys — must match values used in :app when building the launch intent
        private const val INTENT_EXTRA_KEY_HANDLE = "HANDLE"
        private const val INTENT_EXTRA_KEY_ADAPTER_TYPE = "adapterType"
        private const val INTENT_EXTRA_KEY_REBUILD_PLAYLIST = "REBUILD_PLAYLIST"
        private const val INTENT_EXTRA_KEY_CHAT_ID = "chatId"
        private const val INTENT_EXTRA_KEY_MSG_ID = "msgId"
        private const val URL_FILE_LINK = "URL_FILE_LINK"
        private const val URL_LOCAL_FILE_PATH = "URL_LOCAL_FILE_PATH"

        // Adapter type codes — must match values in :app Constants
        private const val INVALID_VALUE = -1
        private const val OFFLINE_ADAPTER = 2004
        private const val FOLDER_LINK_ADAPTER = 2005
        private const val ZIP_ADAPTER = 2008
        private const val FROM_CHAT = 2020
        private const val VERSIONS_ADAPTER = 2035
        private const val FROM_IMAGE_VIEWER = 2036
        private const val FROM_ALBUM_SHARING = 2041
    }
}
