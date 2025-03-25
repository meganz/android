package mega.privacy.android.app.main.drawer

import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Manager drawer ui state
 *
 * @property userChatStatus User status
 * @property backupsNodeHandle Backups node handle
 * @property hasBackupsChildren Has backups children
 * @property canVerifyPhoneNumber Can verify phone number
 * @property isRootNodeExist Is root node exist
 * @property isConnected Is connected
 * @property showPromoTag Boolean to show promo tag for promo notifications
 */
data class ManagerDrawerUiState(
    val userChatStatus: UserChatStatus = UserChatStatus.Invalid,
    val backupsNodeHandle: NodeId = NodeId(-1L),
    val hasBackupsChildren: Boolean = false,
    val canVerifyPhoneNumber: Boolean = false,
    val isRootNodeExist: Boolean = false,
    val isConnected: Boolean = false,
    val showPromoTag: Boolean = false,
)