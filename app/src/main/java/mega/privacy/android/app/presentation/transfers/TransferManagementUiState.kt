package mega.privacy.android.app.presentation.transfers

import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersStatus

/**
 * Transfer management ui state
 *
 * @property transfersInfo
 * @property hideTransfersWidget true if transfers widget should be forced to be hidden regardless of whether
 * there are current transfers or not, in transfers screen for instance
 */
data class TransferManagementUiState(
    val transfersInfo: TransfersInfo = TransfersInfo(),
    val hideTransfersWidget: Boolean = false,
) {
    /**
     * Determines if widget should be visible, active and not hidden
     */
    val widgetVisible =
        transfersInfo.status != TransfersStatus.NotTransferring && !hideTransfersWidget
}