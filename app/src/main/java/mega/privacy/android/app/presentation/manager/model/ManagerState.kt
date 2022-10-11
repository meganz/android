package mega.privacy.android.app.presentation.manager.model

/**
 * Manager UI state
 *
 * @param browserParentHandle current browser parent handle
 * @param rubbishBinParentHandle current rubbish bin parent handle
 * @param inboxParentHandle current inbox parent handle
 * @param isFirstNavigationLevel true if the navigation level is the first level
 * @param sharesTab current tab in shares screen
 * @param transfersTab current tab in transfers screen
 * @param isFirstLogin is first login
 * @param hasInboxChildren whether any nodes for Inbox
 * @param shouldStopCameraUpload camera upload should be stopped or not
 * @param shouldSendCameraBroadcastEvent broadcast event should be sent or not
 */
data class ManagerState(
    val browserParentHandle: Long = -1L,
    val rubbishBinParentHandle: Long = -1L,
    val inboxParentHandle: Long = -1L,
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val transfersTab: TransfersTab = TransfersTab.NONE,
    val isFirstLogin: Boolean = false,
    val hasInboxChildren: Boolean = false,
    val shouldStopCameraUpload: Boolean = false,
    val shouldSendCameraBroadcastEvent: Boolean = false,
)