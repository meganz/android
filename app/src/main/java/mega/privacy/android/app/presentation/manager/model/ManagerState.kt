package mega.privacy.android.app.presentation.manager.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningErrorTypeUiItem
import mega.privacy.android.app.presentation.documentscanner.model.HandleScanDocumentResult
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.RestoreNodeResult


/**
 * Manager state
 *
 * @property isFirstNavigationLevel
 * @property sharesTab
 * @property isFirstLogin
 * @property nodeUpdateReceived
 * @property pendingActionsCount
 * @property shouldAlertUserAboutSecurityUpgrade
 * @property showSyncSection
 * @property show2FADialog
 * @property isPushNotificationSettingsUpdatedEvent
 * @property titleChatArchivedEvent
 * @property restoreNodeResult
 * @property nodeNameCollisionsResult
 * @property moveRequestResult
 * @property message
 * @property chatLinkContent                            Result of check link request
 * @property androidSyncServiceEnabled                  Indicates if need to enable android sync service
 * @property userRootBackupsFolderHandle                The User's Root Backups Folder Handle
 * @property callInProgressChatId                       Chat ID of the current call in progress.
 * @property deviceCenterPreviousBottomNavigationItem   A potentially nullable Integer that holds the previous Bottom Navigation item before accessing Device Center
 * @property callEndedDueToFreePlanLimits               State event to show the force free plan limit participants dialog.
 * @property shouldUpgradeToProPlan                     State to show the upgrade to Pro plan dialog.
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property usersCallLimitReminders                    [UsersCallLimitReminders]
 * @property searchQuery                                Search query
 * @property uploadEvent                                Event to trigger upload actions
 * @property handleScanDocumentResult                   Decides if the legacy or modern Document Scanner should be used
 * @property documentScanningErrorTypeUiItem            The specific Error return when using the modern Document Scanner
 * @property isAndroidSyncWorkManagerFeatureFlagEnabled Feature flag to use Android Sync Work Manager instead of SyncBackgroundService
 * @property adsEnabled                                 Boolean to determine if ads are enabled
 */
data class ManagerState(
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val isFirstLogin: Boolean = false,
    val nodeUpdateReceived: Boolean = false,
    val pendingActionsCount: Int = 0,
    val shouldAlertUserAboutSecurityUpgrade: Boolean = false,
    val showSyncSection: Boolean = false,
    val show2FADialog: Boolean = false,
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val titleChatArchivedEvent: String? = null,
    val restoreNodeResult: Result<RestoreNodeResult>? = null,
    val nodeNameCollisionsResult: NodeNameCollisionsResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val message: InfoToShow? = null,
    val chatLinkContent: Result<ChatLinkContent>? = null,
    val androidSyncServiceEnabled: Boolean = false,
    val userRootBackupsFolderHandle: NodeId = NodeId(-1L),
    val callInProgressChatId: Long = -1L,
    val deviceCenterPreviousBottomNavigationItem: Int? = null,
    val callEndedDueToFreePlanLimits: Boolean = false,
    val shouldUpgradeToProPlan: Boolean = false,
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val usersCallLimitReminders: UsersCallLimitReminders = UsersCallLimitReminders.Enabled,
    val searchQuery: String = "",
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
    val handleScanDocumentResult: HandleScanDocumentResult? = null,
    val documentScanningErrorTypeUiItem: DocumentScanningErrorTypeUiItem? = null,
    val isAndroidSyncWorkManagerFeatureFlagEnabled: Boolean = false,
    val adsEnabled: Boolean = false,
)
