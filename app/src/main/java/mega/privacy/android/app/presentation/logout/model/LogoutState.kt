package mega.privacy.android.app.presentation.logout.model

/**
 * Logout state
 */
sealed interface LogoutState {

    /**
     * Loading
     */
    object Loading : LogoutState

    /**
     * Data
     *
     * @property hasOfflineFiles
     * @property hasPendingTransfers
     */
    data class Data(
        val hasOfflineFiles: Boolean,
        val hasPendingTransfers: Boolean,
    ) : LogoutState
}

