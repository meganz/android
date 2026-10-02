package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * A pending sync transfer the store tracks.
 * [optimisticPause] is a row pause (true) or resume (false) the SDK has not confirmed yet.
 */
internal data class SyncTransferRow(
    val entry: SyncTransferEntry,
    val mediaType: SyncMediaType,
    val optimisticPause: Boolean?,
)

/** A finished sync transfer, kept until cleared, a session reset or process death. */
internal data class SyncCompletedTransfer(
    val entry: SyncTransferEntry,
    val mediaType: SyncMediaType,
    val finishedAtMillis: Long,
    val batchId: Long,
)
