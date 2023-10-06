package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
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
 * @property enabledFlags
 * @property isPushNotificationSettingsUpdatedEvent
 * @property titleChatArchivedEvent
 * @property restoreNodeResult
 * @property nodeNameCollisionResult
 * @property moveRequestResult
 * @property message
 * @property chatLinkContent Result of check link request
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
    val enabledFlags: Set<Feature> = emptySet(),
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val titleChatArchivedEvent: String? = null,
    val restoreNodeResult: Result<RestoreNodeResult>? = null,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val message: String? = null,
    val chatLinkContent: Result<ChatLinkContent>? = null,
)
