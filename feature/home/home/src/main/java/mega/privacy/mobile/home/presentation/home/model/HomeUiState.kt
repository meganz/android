package mega.privacy.mobile.home.presentation.home.model

import androidx.compose.runtime.Stable

@Stable
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Data(
        val widgets: List<HomeWidgetItem>,
        val isSearchRevampEnabled: Boolean = false,
    ) : HomeUiState

    data class Offline(
        val hasOfflineFiles: Boolean,
    ) : HomeUiState
}