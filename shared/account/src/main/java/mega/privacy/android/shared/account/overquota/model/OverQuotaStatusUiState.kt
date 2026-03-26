package mega.privacy.android.shared.account.overquota.model

import androidx.compose.runtime.Stable

/**
 * UI state for OverQuotaStatus
 */
@Stable
sealed interface OverQuotaStatusUiState {
    val overQuotaStatus: OverQuotaStatus
    val shouldShowWarning: Boolean

    /**
     * Loading state
     */
    data object Loading : OverQuotaStatusUiState {
        override val overQuotaStatus = OverQuotaStatus(
            storage = OverQuotaIssue.Storage.None,
            transfer = OverQuotaIssue.Transfer.None,
        )
        override val shouldShowWarning = false
    }

    /**
     * Data state
     *
     * @param overQuotaStatus the current over quota status
     * @param shouldShowWarning true if the warning banner should be shown
     */
    data class Data(
        override val overQuotaStatus: OverQuotaStatus,
        override val shouldShowWarning: Boolean,
    ) : OverQuotaStatusUiState
}
