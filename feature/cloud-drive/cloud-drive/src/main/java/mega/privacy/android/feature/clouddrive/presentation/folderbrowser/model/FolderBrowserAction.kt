package mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI actions of the folder browser screen
 */
sealed interface FolderBrowserAction {

    /**
     * @property newViewType the view type to switch to
     */
    data class ChangeViewTypeClicked(val newViewType: ViewType) : FolderBrowserAction

    data object NavigateBackEventConsumed : FolderBrowserAction

    /**
     * The item indices currently on screen, from the list
     *
     * @property range first..last visible item index
     */
    data class VisibleRangeChanged(val range: IntRange) : FolderBrowserAction
}
