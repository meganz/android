package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.domain.entity.transfer.TransferState

/** Whether the state holds a transfer slot. An ACTIVE WebSocket upload counts by its state, even with no SDK slot. */
internal val TransferState.isSlotHolding: Boolean
    get() = when (this) {
        TransferState.STATE_ACTIVE,
        TransferState.STATE_RETRYING,
        TransferState.STATE_COMPLETING,
            -> true

        TransferState.STATE_NONE,
        TransferState.STATE_QUEUED,
        TransferState.STATE_PAUSED,
        TransferState.STATE_COMPLETED,
        TransferState.STATE_FAILED,
        TransferState.STATE_CANCELLED,
            -> false
    }

/** Whether the state ends the transfer, so a seed skips it and an event completes or rolls it back. */
internal val TransferState.isTerminal: Boolean
    get() = when (this) {
        TransferState.STATE_COMPLETED,
        TransferState.STATE_FAILED,
        TransferState.STATE_CANCELLED,
            -> true

        TransferState.STATE_NONE,
        TransferState.STATE_QUEUED,
        TransferState.STATE_PAUSED,
        TransferState.STATE_ACTIVE,
        TransferState.STATE_RETRYING,
        TransferState.STATE_COMPLETING,
            -> false
    }
