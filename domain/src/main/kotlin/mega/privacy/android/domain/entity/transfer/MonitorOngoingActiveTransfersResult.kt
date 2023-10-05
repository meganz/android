package mega.privacy.android.domain.entity.transfer

/**
 * Data class to return required data in Monitor OngoingActive Transfers UseCases
 * @param activeTransferTotals [ActiveTransferTotals]
 * @param paused true if transfers are paused globally or all individual transfers are paused
 * @param overQuota true if there the transfers are currently on over quota
 */
data class MonitorOngoingActiveTransfersResult(
    val activeTransferTotals: ActiveTransferTotals,
    val paused: Boolean,
    val overQuota: Boolean
)