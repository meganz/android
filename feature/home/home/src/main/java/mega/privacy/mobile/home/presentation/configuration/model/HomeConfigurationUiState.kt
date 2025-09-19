package mega.privacy.mobile.home.presentation.configuration.model

import androidx.compose.runtime.Stable

@Stable
sealed interface HomeConfigurationUiState {
    data object Loading : HomeConfigurationUiState
}