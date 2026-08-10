package mega.privacy.mobile.home.presentation.whatsnew

sealed interface WhatsNewUiState {
    data object Loading : WhatsNewUiState

    data class Ready(val whatsNewDetail: WhatsNewDetail) : WhatsNewUiState
}