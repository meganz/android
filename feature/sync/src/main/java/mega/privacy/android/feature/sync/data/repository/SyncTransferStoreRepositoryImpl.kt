package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.feature.sync.data.feed.SyncTransferFeed
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreCommand
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Holds the in-memory sync transfer store for the process and the commands its run folds.
 * A reset starts a new generation that makes every older command and state stale. Building it
 * starts nothing, and nothing is persisted.
 */
@Singleton
internal class SyncTransferStoreRepositoryImpl @Inject constructor(
    private val syncTransferFeed: SyncTransferFeed,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SyncTransferStoreRepository {

    private val published = MutableStateFlow(SyncTransferStoreState.NotSeeded)
    private val store: StateFlow<SyncTransferStoreState> = published.asStateFlow()
    private val currentGeneration = MutableStateFlow(0L)

    // Keeps a reset from landing between a publish's generation check and its write.
    private val publishLock = Mutex()

    // Held by the repository, so a command sent before the run collects waits for it.
    private val commands = Channel<SyncTransferStoreCommand>(Channel.UNLIMITED)

    private val reseedRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun monitorSyncTransferStore(): Flow<SyncTransferStoreState> = store

    override fun monitorSyncTransferStoreCommands(): Flow<SyncTransferStoreCommand> = channelFlow {
        launch {
            currentGeneration.collectLatest { generation ->
                feedUpdates().collect { update ->
                    sendIfCurrent(SyncTransferStoreCommand(generation, SyncTransferStoreInput.Feed(update)))
                }
            }
        }
        for (command in commands) sendIfCurrent(command)
    }.flowOn(defaultDispatcher)

    // A re-collected feed opens with a Seed in the same generation, which the run folds as a
    // reseed, so completed rows survive a transient feed error.
    private fun feedUpdates(): Flow<SyncTransferUpdate> {
        var backoff = Duration.ZERO
        return syncTransferFeed.updates(reseedRequests)
            .onEach { backoff = Duration.ZERO }
            .retryWhen { cause, _ ->
                if (cause is CancellationException) return@retryWhen false
                Timber.e(cause, "Sync transfer feed failed, retrying in %s", backoff)
                delay(backoff)
                backoff = (backoff * 2).coerceIn(100.milliseconds, 10.minutes)
                true
            }
    }

    // A batch or command from before the last reset must not refill the store.
    private suspend fun ProducerScope<SyncTransferStoreCommand>.sendIfCurrent(
        command: SyncTransferStoreCommand,
    ) {
        if (command.generation >= currentGeneration.value) send(command)
    }

    override suspend fun publishSyncTransferStore(state: SyncTransferStoreState, generation: Long) {
        publishLock.withLock {
            if (generation == currentGeneration.value) published.value = state
        }
    }

    override suspend fun clearCompleted(tags: Set<Int>) =
        enqueue(SyncTransferStoreInput.ClearCompleted(tags))

    override suspend fun clearAllCompleted() = enqueue(SyncTransferStoreInput.ClearAllCompleted)

    override suspend fun setOptimisticPause(tags: Set<Int>, isPaused: Boolean) =
        enqueue(SyncTransferStoreInput.SetOptimisticPause(tags, isPaused))

    override suspend fun revertOptimisticPause(tag: Int) =
        enqueue(SyncTransferStoreInput.RevertOptimisticPause(tag))

    override suspend fun requestReseed() {
        reseedRequests.emit(Unit)
    }

    override suspend fun reset() {
        publishLock.withLock {
            currentGeneration.value += 1
            published.value = SyncTransferStore.reset()
        }
    }

    // Stamped at call time, so a command issued before a reset is dropped after it.
    private suspend fun enqueue(input: SyncTransferStoreInput) {
        commands.send(SyncTransferStoreCommand(currentGeneration.value, input))
    }
}
