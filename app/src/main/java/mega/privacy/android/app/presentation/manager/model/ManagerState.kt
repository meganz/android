package mega.privacy.android.app.presentation.manager.model

/**
 * Manager UI state
 *
 * @param isFirstNavigationLevel true if the navigation level is the first level
 * @param sharesTab current tab in shares screen
 * @param transfersTab current tab in transfers screen
 * @param isFirstLogin is first login
 * @param hasInboxChildren whether any nodes for Inbox
 * @param shouldStopCameraUpload camera upload should be stopped or not
 * @param shouldSendCameraBroadcastEvent broadcast event should be sent or not
 * @param nodeUpdateReceived one-off event to notify UI that a node update occurred
 * @param isMandatoryFingerprintVerificationNeeded Boolean to get if mandatory finger print verification Needed
 * @param pendingActionsCount Pending actions count
 * @param shouldAlertUserAboutSecurityUpgrade Boolean to decide whether to display security upgrade dialog or not
 */
data class ManagerState(
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val transfersTab: TransfersTab = TransfersTab.NONE,
    val isFirstLogin: Boolean = false,
    val hasInboxChildren: Boolean = false,
    val shouldStopCameraUpload: Boolean = false,
    val shouldSendCameraBroadcastEvent: Boolean = false,
    val nodeUpdateReceived: Boolean = false,
    val isMandatoryFingerprintVerificationNeeded: Boolean = false,
    val pendingActionsCount: Int = 0,
    val shouldAlertUserAboutSecurityUpgrade: Boolean = false,
)
