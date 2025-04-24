package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier

/**
 * Domain implementation of active transfer action group
 */
data class ActiveTransferActionGroupImpl(
    override val groupId: Int? = null,
    override val transferType: TransferType,
    override val destination: String,
    override val startTime: Long,
    override val pendingTransferNodeId: PendingTransferNodeIdentifier? = null,
) : ActiveTransferActionGroup