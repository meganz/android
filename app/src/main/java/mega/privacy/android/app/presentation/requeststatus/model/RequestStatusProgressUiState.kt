package mega.privacy.android.app.presentation.requeststatus.model

/**
 * UI state for request status progress bar
 *
 * @property progress Progress of the request status, 0 to 1000
 */
data class RequestStatusProgressUiState(
    val progress: Long = -1L,
) {
    /**
     * Whether to show the progress bar, hide when -1L
     */
    val showProgressBar: Boolean
        get() = progress > -1
}