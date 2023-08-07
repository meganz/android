package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.RestoreNodeResult


/**
 * Manager state
 *
 * @property isFirstNavigationLevel
 * @property sharesTab
 * @property transfersTab
 * @property isFirstLogin
 * @property hasInboxChildren
 * @property nodeUpdateReceived
 * @property pendingActionsCount
 * @property shouldAlertUserAboutSecurityUpgrade
 * @property showSyncSection
 * @property show2FADialog
 * @property canVerifyPhoneNumber
 * @property enabledFlags
 * @property isPushNotificationSettingsUpdatedEvent
 * @property titleChatArchivedEvent
 * @property cancelTransfersResult
 * @property restoreNodeResult
 * @property nodeNameCollisionResult
 * @property moveRequestResult
 */
data class ManagerState(
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val isFirstLogin: Boolean = false,
    val hasInboxChildren: Boolean = false,
    val nodeUpdateReceived: Boolean = false,
    val pendingActionsCount: Int = 0,
    val shouldAlertUserAboutSecurityUpgrade: Boolean = false,
    val showSyncSection: Boolean = false,
    val show2FADialog: Boolean = false,
    val canVerifyPhoneNumber: Boolean = false,
    val enabledFlags: Set<Feature> = emptySet(),
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val titleChatArchivedEvent: String? = null,
    val restoreNodeResult: Result<RestoreNodeResult>? = null,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null
)
