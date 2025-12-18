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
@Parcelize
data class MyAccountNavKey(
    val action: String? = null,
    val link: String? = null,
    val resultCode: Int = -1,
) : NavKey, Parcelable

@Serializable
data object AchievementNavKey : NavKey

@Serializable
data class WebSiteNavKey(val url: String) : NoSessionNavKey.Optional

@Serializable
@Parcelize
data class ContactsNavKey(val navType: NavType = NavType.List) : NavKey, Parcelable {
    @Keep
    enum class NavType {
        List,
        SentRequests,
        ReceivedRequests,
    }
}

/**
 * Navigation key for ChatHostActivity
 * Supports all variations of intent extras used to launch ChatHostActivity
 *
 * @param chatId Chat ID to open (required if openChatList is false)
 * @param action Intent action string (e.g., ACTION_CHAT_SHOW_MESSAGES)
 * @param link Chat link if opened from a link
 * @param snackbarText Text to show in snackbar
 * @param messageId Message ID
 * @param isOverQuota Over quota indicator
 * @param openChatList True to open chat list instead of specific chat
 * @param createNewChat True if the Chat List screen should open with the Create New Chat flow
 * @param flags Intent flags (e.g., FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP)
 */
@Serializable
@Parcelize
data class ChatNavKey(
    val chatId: Long,
    val action: String? = null,
    val link: String? = null,
    val snackbarText: String? = null,
    val messageId: Long? = null,
    val isOverQuota: Int? = null,
) : NoSessionNavKey.Optional, Parcelable

/**
 * Navigation key for Chat List
 *
 * @param createNewChat True if the Chat List screen should open with the Create New Chat flow
 */
@Serializable
@Parcelize
data class ChatListNavKey(
    val createNewChat: Boolean = false,
) : NavKey, Parcelable

/**
 * Navigation key for ManageChatHistoryActivity
 *
 * @param chatId Chat ID of the chat or meeting room (required)
 * @param email Email of the current user (optional)
 */
@Serializable
@Parcelize
data class ManageChatHistoryNavKey(
    val chatId: Long,
    val email: String? = null,
) : NavKey, Parcelable

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
data class FileInfoNavKey(val handle: Long) : NavKey

@Serializable
data class AuthenticityCredentialsNavKey(
    val email: String,
    val isIncomingShares: Boolean,
) : NavKey

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
@Parcelize
data class UpgradeAccountNavKey(
    val isUpgrade: Boolean = true,
    val isNewAccount: Boolean = false,
    val source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
) : NavKey, Parcelable

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
data class AlbumContentPreviewNavKey(
    val albumId: Long?,
    val photoId: Long,
    val albumType: String,
    val sortType: String,
    val title: String,
) : NavKey

@Serializable
data class LegacyAlbumCoverSelectionNavKey(val albumId: Long) : NavKey {
    companion object Companion {
        const val MESSAGE = "extra_message"
    }
}

@Serializable
data class LegacyPhotoSelectionNavKey(
    val albumId: Long,
    val selectionMode: Int,
    val captureResult: Boolean = true,
) : NavKey {
    companion object Companion {
        const val RESULT = "extra_result"
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

@Serializable
data object VideoSectionNavKey : NavKey

@Serializable
data class MediaTimelinePhotoPreviewNavKey(
    val id: Long,
    val sortType: String,
    val filterType: String,
    val mediaSource: String,
) : NavKey

@Serializable
data class LegacyAddToAlbumActivityNavKey(
    val photoIds: List<Long>,
    val viewType: Int, // 0 => Album, 1 => Albums & Playlists
) : NavKey {

    companion object {
        const val ADD_TO_ALBUM_RESULT = "ADD_TO_ALBUM_RESULT"
    }
}

@Serializable
data object LegacyPhotosSearchNavKey : NavKey {
    const val RESULT = "LegacyPhotosSearchNavKey:key_result"
}

@Serializable
data class LegacySettingsCameraUploadsActivityNavKey(
    val isShowHowToUploadPrompt: Boolean = false,
) : NavKey

/**
 * Navigation key for GetLinkActivity that handles legacy navigation.
 *
 * @param handles List of node handles to get their links. If empty, the navigation destination will skip launching the Activity.
 */
@Serializable
data class GetLinkNavKey(
    val handles: List<Long> = emptyList(),
) : NavKey

/**
 * Navigation key for SaveScannedDocumentsActivity that handles legacy navigation.
 *
 * @param originatedFromChat True if the scan originated from chat
 * @param cloudDriveParentHandle The parent handle used when saving to Cloud Drive
 * @param scanPdfUri String representation of the PDF Uri generated by the scanner
 * @param scanSoloImageUri String representation of the single image Uri if available
 */
@Serializable
data class SaveScannedDocumentsNavKey(
    val originatedFromChat: Boolean = false,
    val cloudDriveParentHandle: Long? = null,
    val scanPdfUri: String,
    val scanSoloImageUri: String? = null,
) : NavKey

@Serializable
data class InviteContactNavKey(
    val isFromAchievement: Boolean = false,
) : NavKey
