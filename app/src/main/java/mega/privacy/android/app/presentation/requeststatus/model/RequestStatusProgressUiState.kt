package mega.privacy.android.app.presentation.requeststatus.model

import mega.privacy.android.domain.entity.Progress

/**
 * UI state for request status progress bar
 *
 * @property progress [Progress] of the request status
 */
data class RequestStatusProgressUiState(
    val progress: Progress? = null,
)