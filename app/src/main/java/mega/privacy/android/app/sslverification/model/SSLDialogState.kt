package mega.privacy.android.app.sslverification.model

sealed interface SSLDialogState {
    data object Loading : SSLDialogState
    data class Ready(val webUrl: String) : SSLDialogState
}