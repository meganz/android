package mega.privacy.android.feature.contact.requests.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

/**
 * UI state for the contact requests screen.
 */
@Stable
sealed interface ContactRequestsUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ContactRequestsUiState

    /**
     * Data state containing the received and sent requests and the active tab.
     *
     * @property received Incoming contact requests, filtered by [query] when one is set.
     * @property sent Outgoing contact requests, filtered by [query] when one is set.
     * @property selectedTab Currently selected tab.
     * @property query Active search query, or null when not filtering.
     */
    data class Data(
        val received: ImmutableList<ContactRequestUiItem>,
        val sent: ImmutableList<ContactRequestUiItem>,
        val selectedTab: ContactRequestTab,
        val query: String?,
    ) : ContactRequestsUiState
}
