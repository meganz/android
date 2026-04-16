package mega.privacy.android.feature.transfers.presentation.settings.model

import androidx.compose.runtime.Stable

/**
 * UI state for transfers settings.
 */
@Stable
sealed interface TransfersSettingsUiState {

    /**
     * Initial loading state.
     */
    data object Loading : TransfersSettingsUiState

    /**
     * Data state.
     *
     * @property maxDownloadConnections Max number of download connections.
     * @property maxUploadConnections Max number of upload connections.
     * @property maxTransferConnectionsRange Valid range for max transfer connections.
     */
    data class Data(
        val maxDownloadConnections: Int,
        val maxUploadConnections: Int,
        val maxTransferConnectionsRange: IntRange,
    ) : TransfersSettingsUiState
}
