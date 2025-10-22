package mega.privacy.android.navigation.destination

import androidx.annotation.Keep
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.sync.SyncType

@Serializable
data object OverDiskQuotaPaywallWarningNavKey : NavKey

@Serializable
data object MyAccountNavKey : NavKey

@Serializable
data object AchievementNavKey : NavKey

@Serializable
data class WebSiteNavKey(val url: String) : NavKey

@Serializable
data class ContactsNavKey(val navType: NavType = NavType.List) : NavKey {
    @Keep
    enum class NavType {
        List,
        SentRequests,
        ReceivedRequests,
    }
}

@Serializable
data class ChatNavKey(val chatId: Long, val action: String?) : NavKey

@Serializable
data object ChatsNavKey : NavKey

@Serializable
data class AddContactToShareNavKey(
    val contactType: ContactType,
    val nodeHandle: List<Long>,
) : NavKey {
    @Keep
    enum class ContactType {
        Mega,
        Device,
        All,
    }

    companion object Companion {
        const val KEY = "extra_contacts"
    }
}

@Serializable
data class ContactInfoNavKey(val email: String) : NavKey

@Serializable
data class FileContactInfoNavKey(
    val folderHandle: Long,
    val folderName: String,
) : NavKey

@Serializable
data class SearchNodeNavKey(
    val isFirstNavigationLevel: Boolean,
    val nodeSourceType: NodeSourceType,
    val parentHandle: Long,
) : NavKey

@Serializable
data class TestPasswordNavKey(
    val wrongPasswordCounter: Int = 0,
    val isTestPasswordMode: Boolean = false,
    val isLogoutMode: Boolean = false,
) : NavKey

@Serializable
data class OfflineInfoNavKey(val handle: String) : NavKey

@Serializable
data object SyncListNavKey : NavKey

@Serializable
data class SyncNewFolderNavKey(
    val syncType: SyncType = SyncType.TYPE_TWOWAY,
    val isFromManagerActivity: Boolean = false,
    val isFromCloudDrive: Boolean = false,
    val remoteFolderHandle: Long? = null,
    val remoteFolderName: String? = null,
) : NavKey

@Serializable
data class SyncSelectStopBackupDestinationNavKey(
    val folderName: String? = null,
) : NavKey