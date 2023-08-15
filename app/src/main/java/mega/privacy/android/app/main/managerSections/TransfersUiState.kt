package mega.privacy.android.app.main.managerSections

import mega.privacy.android.domain.entity.transfer.Transfer

/**
 * Transfer ui state
 *
 * @property pauseOrResumeTransferResult
 * @property cancelTransfersResult
 */
data class TransfersUiState(
    val pauseOrResumeTransferResult: Result<Transfer>? = null,
    val cancelTransfersResult: Result<Unit>? = null
)