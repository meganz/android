package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.data.forEach
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.progress.SyncStatsSampleMapper
import mega.privacy.android.feature.sync.data.mapper.progress.SyncWorkStateMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkStatesUpdate
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import nz.mega.sdk.MegaSyncStats
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the app-wide run snapshot, so every surface shares one run baseline, and reads the
 * existing sync listener into the sync states its run folds. Building it starts nothing.
 */
@Singleton
internal class SyncProgressRepositoryImpl @Inject constructor(
    private val syncGateway: SyncGateway,
    private val syncStatsCacheGateway: SyncStatsCacheGateway,
    private val syncWorkStateMapper: SyncWorkStateMapper,
    private val syncStatsSampleMapper: SyncStatsSampleMapper,
    @SystemTime private val currentTimeProvider: () -> Long,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncProgressRepository {

    private val published = MutableStateFlow(SyncRunSnapshot.NotSeeded)
    private val runSnapshot: StateFlow<SyncRunSnapshot> = published.asStateFlow()
    private val currentGeneration = MutableStateFlow(0L)

    // Keeps a reset from landing between a publish's generation check and its write.
    private val publishLock = Mutex()

    override fun monitorSyncRunSnapshot(): Flow<SyncRunSnapshot> = runSnapshot

    override fun monitorSyncWorkStates(): Flow<SyncWorkStatesUpdate> = channelFlow {
        val events = Channel<MegaSyncListenerEvent>(Channel.UNLIMITED)
        // One subscription for the whole collection: a reset must not re-register the SDK listener,
        // and every seed read must find it already subscribed.
        launch(start = CoroutineStart.UNDISPATCHED) {
            syncGateway.syncUpdate.collect { events.send(it) }
        }
        var fold: Job? = null
        currentGeneration.collect { generation ->
            fold?.cancelAndJoin()
            fold = launch(start = CoroutineStart.UNDISPATCHED) {
                // A reset can change the account, so an event from before it must not be applied;
                // the seed read below already covers it.
                while (events.tryReceive().isSuccess) Unit
                syncWorkStates(events).collect { send(SyncWorkStatesUpdate(generation, it)) }
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun publishSyncRunSnapshot(snapshot: SyncRunSnapshot, generation: Long) {
        publishLock.withLock {
            if (generation == currentGeneration.value) published.value = snapshot
        }
    }

    override suspend fun resetSyncRunSnapshot() {
        publishLock.withLock {
            currentGeneration.value += 1
            published.value = SyncRunSnapshot.NotSeeded
        }
    }

    // Null until the seed read succeeds, so an unreadable seed is retried at the next event.
    private fun syncWorkStates(events: ReceiveChannel<MegaSyncListenerEvent>): Flow<List<SyncWorkState>> =
        flow {
            emit(Input.Refresh)
            for (event in events) emit(Input.Event(event))
        }
            .runningFold<Input, Map<Long, SyncWorkState>?>(null) { syncs, input -> reduce(syncs, input) }
            .filterNotNull()
            .map { it.values.toList() }

    private suspend fun reduce(syncs: Map<Long, SyncWorkState>?, input: Input): Map<Long, SyncWorkState>? {
        val now = currentTimeProvider()
        val seeded = syncs ?: readSyncList(previous = emptyMap(), isSeed = true, now = now) ?: return null
        return when (input) {
            Input.Refresh -> seeded
            is Input.Event -> apply(seeded, input.event, now)
        }
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

    private sealed interface Input {
        data object Refresh : Input
        data class Event(val event: MegaSyncListenerEvent) : Input
    }
}
