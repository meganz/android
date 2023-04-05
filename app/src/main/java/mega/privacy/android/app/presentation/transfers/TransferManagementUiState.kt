package mega.privacy.android.app.presentation.transfers

import mega.privacy.android.domain.entity.TransfersInfo

/**
 * Transfer management ui state
 *
 * @property transfersInfo
 */
data class TransferManagementUiState(
    val transfersInfo: TransfersInfo = TransfersInfo(),
)