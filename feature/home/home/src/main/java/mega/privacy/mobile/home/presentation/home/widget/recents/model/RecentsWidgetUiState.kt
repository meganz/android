package mega.privacy.mobile.home.presentation.home.widget.recents.model

/**
 * Recents widget UI state
 *
 * @param recentActionItems list of recent action bucket UI entities
 * @param isLoading true if loading
 * @param isHideRecentsEnabled true if hide recents actions is enabled in settings
 */
data class RecentsWidgetUiState(
    val recentActionItems: List<RecentsUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val isHideRecentsEnabled: Boolean = false,
)
