package mega.privacy.android.feature.contact.add.model

/**
 * Add contact ui state
 */
sealed interface AddContactUiState {
    /**
     * Loading
     */
    data object Loading : AddContactUiState

    /**
     * Data
     */
    data object Data : AddContactUiState
}