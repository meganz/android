package mega.privacy.android.domain.entity.transfer

/**
 * Domain implementation of active transfer action group
 */
data class ActiveTransferActionGroupImpl(
    override val groupId: Int? = null,
    override val transferType: TransferType,
    override val destination: String,
    override val startTime: Long,
) : ActiveTransferActionGroup