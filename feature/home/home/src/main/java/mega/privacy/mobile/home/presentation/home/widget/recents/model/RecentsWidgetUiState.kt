package mega.privacy.mobile.home.presentation.home.widget.recents.model

/**
 * Recents widget UI state
 *
 * @param recentActionItems list of recent action bucket UI entities
 * @param isLoading true if loading
 */
data class RecentsWidgetUiState(
    val recentActionItems: List<RecentsUiItem> = emptyList(),
    val isLoading: Boolean = true,
)
