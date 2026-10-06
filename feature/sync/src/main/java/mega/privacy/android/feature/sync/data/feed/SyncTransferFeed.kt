package mega.privacy.android.feature.sync.data.feed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.mapper.progress.SyncTransferMapper
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Feeds the sync transfer store from the shared SDK transfer flow.
 * It subscribes, and waits for the SDK listener to be registered, before reading the snapshot,
 * so a finish can never fall between the two; a later re-registration triggers a reseed.
 * Raw events are conflated per tag before mapping, so a progress tick costs two getters,
 * not a full mapping, even in worker processes with no screen open.
 */
internal class SyncTransferFeed @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val syncTransferMapper: SyncTransferMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Cold. Subscribes to transfer events, then snapshots; emits Seed, then conflated Batches
     * in SDK order, and a Reseed per request.
     */
    fun updates(reseedRequests: Flow<Unit> = emptyFlow()): Flow<SyncTransferUpdate> = channelFlow {
        val early = Channel<FeedItem>(Channel.UNLIMITED)
        launch(start = CoroutineStart.UNDISPATCHED) {
            megaApiGateway.globalTransfer.collect { event ->
                event.syncFileEventType()?.let { early.trySend(FeedItem.Raw(event, it)) }
            }
        }
        launch { reseedRequests.collect { early.trySend(FeedItem.ReseedMarker) } }
        // For the first collector, shareIn registers the SDK listener asynchronously.
        val generation = megaApiGateway.globalTransferListenerGeneration.first { it > 0 }
        // A later registration means the listener was replaced, so events may have been missed.
        launch {
            megaApiGateway.globalTransferListenerGeneration
                // Compare to the captured value, not drop(1): a re-registration between
                // `first` and this subscription is already current and must still reseed.
                .filter { it > generation }
                .collect { early.trySend(FeedItem.ReseedMarker) }
        }

        val seed = snapshot()
        var watermark = seed.highestNotificationNumber()
        send(SyncTransferUpdate.Seed(seed))

        val chunk = ArrayList<FeedItem.Raw>()
        var chunkId = 0L
        var chunkTimer: Job? = null
        for (item in early) {
            when (item) {
                is FeedItem.Raw -> {
                    if (chunk.isEmpty()) {
                        val id = ++chunkId
                        chunkTimer = launch {
                            delay(CHUNK)
                            early.trySend(FeedItem.ChunkElapsed(id))
                        }
                    }
                    chunk += item
                }

                is FeedItem.ChunkElapsed -> if (item.chunkId == chunkId && chunk.isNotEmpty()) {
                    watermark = maxOf(watermark, sendBatch(chunk))
                    chunk.clear()
                }

                FeedItem.ReseedMarker -> {
                    chunkTimer?.cancel()
                    if (chunk.isNotEmpty()) {
                        watermark = maxOf(watermark, sendBatch(chunk))
                        chunk.clear()
                    }
                    val entries = snapshot()
                    send(SyncTransferUpdate.Reseed(entries, watermark))
                    watermark = maxOf(watermark, entries.highestNotificationNumber())
                }
            }
        }
    }.buffer(Channel.UNLIMITED).flowOn(ioDispatcher)

    /** Whitelist first, so other subtypes never touch their transfer; the folder flag is read only for sync transfers. */
    private fun GlobalTransfer.syncFileEventType(): SyncTransferEventType? = when (this) {
        is GlobalTransfer.OnTransferStart -> SyncTransferEventType.Start
        is GlobalTransfer.OnTransferUpdate -> SyncTransferEventType.Update
        is GlobalTransfer.OnTransferTemporaryError -> SyncTransferEventType.TemporaryError
        is GlobalTransfer.OnTransferFinish -> SyncTransferEventType.Finish
        else -> null
    }?.takeIf { transfer.isSyncTransfer && !transfer.isFolderTransfer }

    /** Keeps one event per tag, maps only those and sends them in SDK order; returns the highest number read. */
    private suspend fun ProducerScope<SyncTransferUpdate>.sendBatch(raws: List<FeedItem.Raw>): Long {
        val survivors = LinkedHashMap<Int, Survivor>()
        var highest = 0L
        for (raw in raws) {
            val (tag, number) = runCatching {
                raw.event.transfer.tag to raw.event.transfer.notificationNumber
            }.onFailure { Timber.w(it, "Unreadable sync transfer event dropped") }
                .getOrNull() ?: continue
            highest = maxOf(highest, number)
            val previous = survivors[tag]
            val start = if (raw.type == SyncTransferEventType.Start) {
                minOf(number, previous?.startNumber ?: number)
            } else {
                previous?.startNumber
            }
            survivors[tag] = when {
                previous == null -> Survivor(raw, number, start)
                previous.raw.type == SyncTransferEventType.Finish -> previous.copy(startNumber = start)
                raw.type == SyncTransferEventType.Finish || number > previous.number ->
                    Survivor(raw, number, start)

                else -> previous.copy(startNumber = start)
            }
        }
        val events = survivors.values.sortedBy { it.number }.mapNotNull { survivor ->
            runCatching {
                val type = survivor.raw.type
                SyncTransferEvent(
                    type = type,
                    entry = syncTransferMapper(
                        survivor.raw.event.transfer,
                        readPath = type == SyncTransferEventType.Finish,
                    ),
                    startNotificationNumber = survivor.startNumber,
                )
            }.onFailure { Timber.w(it, "Sync transfer event dropped") }.getOrNull()
        }
        if (events.isNotEmpty()) send(SyncTransferUpdate.Batch(events))
        return highest
    }

    private suspend fun snapshot(): List<SyncTransferEntry> {
        val transfers = runCatching {
            megaApiGateway.getTransfers(MegaTransfer.TYPE_UPLOAD) +
                    megaApiGateway.getTransfers(MegaTransfer.TYPE_DOWNLOAD)
        }.onFailure {
            if (it !is CancellationException) Timber.w(it, "Sync transfer snapshot failed")
        }.getOrThrow()
        return transfers
            .filter { it.isSyncTransfer && !it.isFolderTransfer }
            .mapNotNull { transfer ->
                runCatching { syncTransferMapper(transfer, readPath = false) }
                    .onFailure { Timber.w(it, "Sync transfer snapshot entry dropped") }
                    .getOrNull()
            }
    }

    private fun List<SyncTransferEntry>.highestNotificationNumber(): Long =
        maxOfOrNull { it.notificationNumber } ?: 0L

    private sealed interface FeedItem {
        data class Raw(val event: GlobalTransfer, val type: SyncTransferEventType) : FeedItem
        data class ChunkElapsed(val chunkId: Long) : FeedItem
        data object ReseedMarker : FeedItem
    }

    private data class Survivor(val raw: FeedItem.Raw, val number: Long, val startNumber: Long?)

    companion object {
        /** How long a chunk stays open after its first sync event. */
        val CHUNK = 1.seconds
    }
}
