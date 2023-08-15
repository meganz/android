package mega.privacy.android.app.main.managerSections

import mega.privacy.android.domain.entity.transfer.Transfer

/**
 * Transfer ui state
 *
 * @property pauseOrResumeTransferResult
 */
data class TransfersUiState(
    val pauseOrResumeTransferResult: Result<Transfer>? = null,
)