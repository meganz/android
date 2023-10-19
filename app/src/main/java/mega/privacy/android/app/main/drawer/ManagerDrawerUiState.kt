package mega.privacy.android.app.main.drawer

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Manager drawer ui state
 *
 * @property userChatStatus User status
 * @property backUpNodeHandle Back up node handle
 * @property hasBackupsChildren Has backups children
 * @property canVerifyPhoneNumber Can verify phone number
 * @property isRootNodeExist Is root node exist
 * @property isConnected Is connected
 * @property enabledFlags Enabled flags
 */
data class ManagerDrawerUiState(
    val userChatStatus: UserChatStatus = UserChatStatus.Invalid,
    val backUpNodeHandle: Long = -1L,
    val hasBackupsChildren: Boolean = false,
    val canVerifyPhoneNumber: Boolean = false,
    val isRootNodeExist: Boolean = false,
    val isConnected: Boolean = false,
    val enabledFlags: Set<Feature> = emptySet(),
)