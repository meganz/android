package mega.privacy.android.feature.sync.domain.entity.progress

/** Desktop's batch pill, or null while the batch counts nothing. */
internal fun SyncTransferCounts.batchProgress(): SyncTransferBatchProgress? {
    val total = batchUploads.total + batchDownloads.total
    if (total == 0) return null
    val ongoing = (if (batchUploads.pending > 0) 1 else 0) + (if (batchDownloads.pending > 0) 1 else 0)
    return SyncTransferBatchProgress(
        current = ongoing + batchUploads.completed + batchDownloads.completed,
        total = total,
    )
}
