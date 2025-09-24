package mega.privacy.android.navigation.destination

import androidx.annotation.Keep
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType

@Serializable
data object OverDiskQuotaPaywallWarning : NavKey

@Serializable
data object MyAccount : NavKey

@Serializable
data object Achievement : NavKey

@Serializable
data class WebSite(val url: String) : NavKey

@Serializable
data object Contacts : NavKey

@Serializable
data class Chat(val chatId: Long, val action: String?) : NavKey

@Serializable
data class AddContactToShare(
    val contactType: ContactType,
    val nodeHandle: List<Long>,
) : NavKey {
    @Keep
    enum class ContactType {
        Mega,
        Device,
        All,
    }

    companion object {
        const val KEY = "extra_contacts"
    }
}

@Serializable
data class ContactInfo(val email: String) : NavKey

@Serializable
data class FileContactInfo(
    val folderHandle: Long,
    val folderName: String,
) : NavKey

@Serializable
data class SearchNode(
    val isFirstNavigationLevel: Boolean,
    val nodeSourceType: NodeSourceType,
    val parentHandle: Long,
) : NavKey