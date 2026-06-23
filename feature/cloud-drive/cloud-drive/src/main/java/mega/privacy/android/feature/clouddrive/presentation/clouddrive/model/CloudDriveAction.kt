package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Cloud Drive action
 * This interface defines the UI actions that can be performed in the Cloud Drive screen.
 */
sealed interface CloudDriveAction {

    /**
     * Change view type clicked action
     * @property newViewType
     */
    data class ChangeViewTypeClicked(val newViewType: ViewType) : CloudDriveAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : CloudDriveAction

    /**
     * Inactivity banner dismissed action. Acknowledges the purge so it is not shown again.
     * @property purgeTimestamp the Unix timestamp (seconds) of the purge to acknowledge.
     */
    data class InactivityBannerDismissed(val purgeTimestamp: Long) : CloudDriveAction

}