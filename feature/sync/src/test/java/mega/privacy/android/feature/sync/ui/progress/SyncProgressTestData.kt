package mega.privacy.android.feature.sync.ui.progress

import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus

internal fun status(
    phase: SyncProgressPhase = SyncProgressPhase.Transferring,
    isSeeded: Boolean = true,
    pending: Int = 0,
    interrupted: Int = 0,
    processed: Int = 0,
    fraction: Float? = null,
) = SyncProgressStatus(
    isSeeded = isSeeded,
    phase = phase,
    progress = SyncEpisodeProgress(
        episodeId = 1,
        processedItems = processed,
        pendingItems = pending,
        interruptedItems = interrupted,
        fraction = fraction,
    ),
)

internal fun blockers(
    isOffline: Boolean = false,
    isStorageOverQuota: Boolean = false,
    isTransferOverQuota: Boolean = false,
    areTransfersPaused: Boolean = false,
    stalledIssueCount: Int = 0,
) = SyncProgressBlockers(
    isOffline = isOffline,
    isStorageOverQuota = isStorageOverQuota,
    isTransferOverQuota = isTransferOverQuota,
    areTransfersPaused = areTransfersPaused,
    stalledIssueCount = stalledIssueCount,
)

internal fun entry(
    tag: Int,
    direction: SyncTransferDirection = SyncTransferDirection.Upload,
    state: TransferState = TransferState.STATE_ACTIVE,
    fileName: String = "IMG_$tag.jpg",
    totalBytes: Long = 1_000L,
    transferredBytes: Long = 400L,
    speed: Long = 100L,
    meanSpeed: Long = 0L,
    priority: Long = tag.toLong(),
    notificationNumber: Long = tag.toLong(),
    lastError: SyncTransferError? = null,
    isForeignOverQuota: Boolean = false,
    path: String? = null,
    location: String? = null,
) = SyncTransferEntry(
    tag = tag,
    direction = direction,
    state = state,
    fileName = fileName,
    totalBytes = totalBytes,
    transferredBytes = transferredBytes,
    speed = speed,
    meanSpeed = meanSpeed,
    priority = priority,
    notificationNumber = notificationNumber,
    lastError = lastError,
    isForeignOverQuota = isForeignOverQuota,
    path = path,
    location = location,
)

internal fun active(
    entry: SyncTransferEntry,
    status: SyncTransferStatus = SyncTransferStatus.Transferring,
    isOwnPause: Boolean = false,
    optimisticPause: Boolean? = null,
) = SyncTransferListItem.Active(
    row = SyncTransferRow(
        entry = entry,
        mediaType = SyncMediaType.Image,
        optimisticPause = optimisticPause,
    ),
    status = status,
    isOwnPause = isOwnPause,
)

internal fun rowState(
    status: SyncTransferStatus = SyncTransferStatus.Transferring,
    isOwnPause: Boolean = false,
    speed: Long = 100L,
    progressPermille: Int = 400,
    etaSeconds: Long? = null,
) = SyncTransferRowState(
    status = status,
    isOwnPause = isOwnPause,
    speed = speed,
    progressPermille = progressPermille,
    etaSeconds = etaSeconds,
)

internal fun finished(
    entry: SyncTransferEntry,
    finishedAtMillis: Long = 1_727_700_000_123L,
    batchId: Long = 0L,
) = SyncTransferListItem.Finished(
    transfer = SyncCompletedTransfer(
        entry = entry,
        mediaType = SyncMediaType.Image,
        finishedAtMillis = finishedAtMillis,
        batchId = batchId,
    ),
)

internal fun list(
    items: List<SyncTransferListItem> = emptyList(),
    isSeeded: Boolean = true,
    totalCount: Int = items.size,
    counts: SyncTransferFilterCounts = SyncTransferFilterCounts.Empty,
) = SyncTransferList(
    isSeeded = isSeeded,
    items = items.toImmutableList(),
    totalCount = totalCount,
    counts = counts,
)
