package mega.privacy.mobile.home.presentation.home.widget.recents.model

/**
 * Recents widget UI state
 *
 * @param recentActionItems list of recent action bucket UI entities
 * @param isNodesLoading true if nodes are loading
 * @param isHideRecentsEnabled true if hide recents actions is enabled in settings
 * @param isHiddenNodesEnabled true if hidden nodes feature is enabled
 * @param showHiddenNodes true if hidden nodes should be shown
 * @param isHiddenNodeSettingsLoading true if loading due to hidden node settings change
 */
data class RecentsWidgetUiState(
    val recentActionItems: List<RecentsUiItem> = emptyList(),
    val isNodesLoading: Boolean = true,
    val isHideRecentsEnabled: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodeSettingsLoading: Boolean = true,
) {
    /**
     * Whether the recents widget is loading
     */
    val isLoading = isNodesLoading || isHiddenNodeSettingsLoading

    /**
     * Whether to exclude sensitive nodes from list based on hidden nodes settings
     */
    val excludeSensitives = isHiddenNodesEnabled && !showHiddenNodes
}
