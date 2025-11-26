package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.payment.UpgradeAccountSource

@Serializable
data object OverDiskQuotaPaywallWarningNavKey : NavKey

@Serializable
data class MyAccountNavKey(val action: String? = null, val link: String? = null) : NavKey

@Serializable
data object AchievementNavKey : NavKey

@Serializable
data class WebSiteNavKey(val url: String) : NoSessionNavKey.Optional

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
@Parcelize
data class ChatNavKey(val chatId: Long, val action: String?) : NoSessionNavKey.Optional, Parcelable

@Serializable
@Parcelize
data object ChatsNavKey : NavKey, Parcelable

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
@Parcelize
data object SyncListNavKey : NavKey, Parcelable

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

@Serializable
data class UpgradeAccountNavKey(
    val isUpgrade: Boolean = true,
    val isNewAccount: Boolean = false,
    val source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
) : NavKey

@Serializable
@Parcelize
data class LegacyFileExplorerNavKey(
    val uriPath: String?,
    val highlightedFiles: List<String> = emptyList(),
) : NoSessionNavKey.Optional, Parcelable

@Serializable
@Parcelize
data class LegacyZipBrowserNavKey(
    val zipFilePath: String?,
) : NoSessionNavKey.Optional, Parcelable

@Serializable
data class LegacyFileLinkNavKey(
    val uriString: String?,
) : NoSessionNavKey.Optional

@Serializable
data object LegacyExportRecoveryKeyNavKey : NavKey

@Serializable
data class LegacyFolderLinkNavKey(
    val uriString: String?,
) : NoSessionNavKey.Optional

@Serializable
data class LegacyAlbumCoverSelectionNavKey(val albumId: Long) : NavKey {
    companion object Companion {
        const val MESSAGE = "extra_message"
    }
}

@Serializable
data class AlbumGetLinkNavKey(
    val albumId: Long,
    val hasSensitiveContent: Boolean,
) : NavKey

@Serializable
data class LegacyAlbumImportNavKey(val link: String?) : NoSessionNavKey.Optional

@Serializable
data class LegacyPdfViewerNavKey(
    val nodeHandle: Long,
    val nodeContentUri: NodeContentUri,
    val nodeSourceType: Int? = null,
    val mimeType: String,
) : NoSessionNavKey.Optional

@Serializable
data class LegacyImageViewerNavKey(
    val nodeHandle: Long,
    val parentNodeHandle: Long,
    val nodeSourceType: Int? = null,
) : NoSessionNavKey.Optional

@Serializable
data class LegacyTextEditorNavKey(
    val nodeHandle: Long,
    val mode: String,
    val nodeSourceType: Int? = null,
    val fileName: String? = null,
) : NoSessionNavKey.Optional

@Serializable
data class LegacyMediaPlayerNavKey(
    val nodeHandle: Long,
    val nodeContentUri: NodeContentUri,
    val nodeSourceType: Int? = null,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isFolderLink: Boolean = false,
    val fileName: String,
    val parentHandle: Long,
    val fileHandle: Long,
    val fileTypeInfo: FileTypeInfo,
) : NoSessionNavKey.Optional
