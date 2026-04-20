package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property logoutCompletedEvent true if logout is completed
 * @property navigateToSingleActivity true if needs to navigate to MegaActivity with deep link pending to process
 */
data class OpenLinkUiState(
    val logoutCompletedEvent: Boolean = false,
    val navigateToSingleActivity: Boolean = false,
)
