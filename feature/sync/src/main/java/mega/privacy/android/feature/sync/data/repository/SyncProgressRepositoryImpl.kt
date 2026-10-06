package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.data.forEach
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.progress.SyncStatsSampleMapper
import mega.privacy.android.feature.sync.data.mapper.progress.SyncWorkStateMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import mega.privacy.android.feature.sync.domain.usecase.progress.AdvanceSyncProgressEpisodeUseCase
import nz.mega.sdk.MegaSyncStats
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Folds the existing sync listener into one app-wide activity, so every surface shares one run
 * baseline. Nothing is read or collected until the first subscriber.
 */
@Singleton
internal class SyncProgressRepositoryImpl @Inject constructor(
    private val syncGateway: SyncGateway,
    private val syncStatsCacheGateway: SyncStatsCacheGateway,
    private val syncWorkStateMapper: SyncWorkStateMapper,
    private val syncStatsSampleMapper: SyncStatsSampleMapper,
    private val advanceSyncProgressEpisodeUseCase: AdvanceSyncProgressEpisodeUseCase,
    @SystemTime private val currentTimeProvider: () -> Long,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : SyncProgressRepository {

    // With no subscriber an emission is dropped, so a reset never starts the fold.
    private val resetSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Built on first use and never stopped, so the run survives screens and sessions.
    private val activity: StateFlow<SyncActivity> by lazy {
        // Subscribes before the seed read, so no event between the read and the subscription is lost.
        channelFlow {
            launch(start = CoroutineStart.UNDISPATCHED) {
                syncGateway.syncUpdate.collect { send(Input.Event(it)) }
            }
            launch { resetSignal.collect { send(Input.Reset) } }
            send(Input.Refresh)
        }
            .runningFold(State.Initial) { state, input -> reduce(state, input) }
            .map { it.toSyncActivity() }
            .distinctUntilChanged()
            .catch { Timber.e(it, "Sync activity monitoring stopped") }
            .flowOn(ioDispatcher)
            .stateIn(appScope, SharingStarted.Lazily, SyncActivity.NotSeeded)
    }

    override fun monitorSyncActivity(): Flow<SyncActivity> = activity

    override suspend fun resetSyncActivity() {
        resetSignal.emit(Unit)
    }

    private suspend fun reduce(state: State, input: Input): State {
        val now = currentTimeProvider()
        var current = if (input is Input.Reset) State.Initial else state
        if (!current.isSeeded) {
            val seeded = readSyncList(previous = emptyMap(), isSeed = true, now = now)
                ?: return current
            current = current.copy(isSeeded = true, syncs = seeded)
        }
        val next = when (input) {
            Input.Refresh, Input.Reset -> current.syncs
            is Input.Event -> apply(current.syncs, input.event, now)
        }
        return current.copy(
            syncs = next,
            episode = advanceSyncProgressEpisodeUseCase(current.episode, next.values.toList(), now),
        )
    }

    private suspend fun apply(
        syncs: Map<Long, SyncWorkState>,
        event: MegaSyncListenerEvent,
        now: Long,
    ): Map<Long, SyncWorkState> = when (event) {
        is MegaSyncListenerEvent.OnSyncStatsUpdated -> applyStats(syncs, event.syncStats, now)

        is MegaSyncListenerEvent.OnSyncStateChanged -> {
            val (stats, runningSince) = carriedOver(syncs[event.sync.backupId], now)
            syncs + (event.sync.backupId to syncWorkStateMapper(event.sync, stats, runningSince))
        }

        is MegaSyncListenerEvent.OnSyncDeleted -> syncs - event.sync.backupId

        MegaSyncListenerEvent.OnGlobalSyncStateChanged,
        MegaSyncListenerEvent.OnRefreshSyncState,
            -> syncs
    }

    private suspend fun applyStats(
        syncs: Map<Long, SyncWorkState>,
        stats: MegaSyncStats,
        now: Long,
    ): Map<Long, SyncWorkState> {
        val backupId = stats.backupId
        val known = if (backupId in syncs) {
            syncs
        } else {
            readSyncList(previous = syncs, isSeed = false, now = now) ?: return syncs
        }
        val entry = known[backupId]
        return if (entry?.runState == SyncRunState.Running) {
            known + (backupId to entry.copy(stats = syncStatsSampleMapper(stats)))
        } else {
            known
        }
    }

    /**
     * Returns the syncs of a fresh list read, or null when the read fails. Cached stats are read
     * only at the seed: after a suspend the SDK restarts its counters, so a cached entry is stale.
     */
    private suspend fun readSyncList(
        previous: Map<Long, SyncWorkState>,
        isSeed: Boolean,
        now: Long,
    ): Map<Long, SyncWorkState>? {
        val list = try {
            syncGateway.getFolderPairs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not read the sync list")
            return null
        }
        val cached = if (isSeed) readCachedStats() else emptyMap()
        return buildMap {
            list.forEach { sync ->
                val (stats, runningSince) = if (isSeed) {
                    cached[sync.backupId]?.let { syncStatsSampleMapper(it) } to null
                } else {
                    carriedOver(previous[sync.backupId], now)
                }
                put(sync.backupId, syncWorkStateMapper(sync, stats, runningSince))
            }
        }
    }

    private fun readCachedStats(): Map<Long, MegaSyncStats> = try {
        syncStatsCacheGateway.getAllSyncStats().associateBy { it.backupId }
    } catch (e: Exception) {
        Timber.w(e, "Could not read the cached sync stats")
        emptyMap()
    }

    /** A sync staying Running keeps its stats; one entering Running starts without stats, now. */
    private fun carriedOver(previous: SyncWorkState?, now: Long): Pair<SyncStatsSample?, Long?> =
        if (previous?.runState == SyncRunState.Running) {
            previous.stats to previous.runningSinceMillis
        } else {
            null to now
        }

    private data class State(
        val isSeeded: Boolean,
        val syncs: Map<Long, SyncWorkState>,
        val episode: SyncProgressEpisode,
    ) {
        fun toSyncActivity() = SyncActivity(
            isSeeded = isSeeded,
            syncs = syncs.values.sortedBy { it.syncId },
            progress = episode.toProgress(),
        )

        companion object {
            val Initial = State(false, emptyMap(), SyncProgressEpisode.Initial)
        }
    }

    private sealed interface Input {
        data object Refresh : Input
        data object Reset : Input
        data class Event(val event: MegaSyncListenerEvent) : Input
    }
}
