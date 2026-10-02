package mega.privacy.android.feature.sync.domain.entity.progress

import mega.privacy.android.domain.entity.transfer.TransferState

/** A pending sync transfer counted beyond the tracked limit: not listed, state kept for counts and promotion. */
internal data class SyncTransferOverflowEntry(
    val direction: SyncTransferDirection,
    val state: TransferState,
    val mediaType: SyncMediaType = SyncMediaType.Other,
)
