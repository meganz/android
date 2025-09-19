package mega.privacy.mobile.home.presentation.home.model

import androidx.compose.runtime.Stable

@Stable
sealed interface HomeUiState {
    data object Loading : HomeUiState
}