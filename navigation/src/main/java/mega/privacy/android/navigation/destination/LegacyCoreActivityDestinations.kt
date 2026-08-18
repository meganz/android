package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.navigation.payment.SubscriptionOfferSource
import mega.privacy.android.navigation.payment.UpgradeAccountSource

@Serializable
@Parcelize
data object OverDiskQuotaPaywallWarningNavKey : NavKey, Parcelable

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
data class WebSiteNavKey(
    val url: String,
    val isBrowserLink: Boolean = false,
) : NoSessionNavKey.Optional

@Serializable
@Parcelize
data object ContactsNavKey : NavKey, Parcelable

@Serializable
@Parcelize
data class ContactRequestsNavKey(val navType: NavType) : NavKey, Parcelable {
    @Keep
    enum class NavType {
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
) : NoSessionNavKey.Optional, Parcelable {
    companion object {
        @Deprecated("Use NavKeys instead")
        const val LEGACY_CHAT_ID: String = "CHAT_ID"

        @Deprecated("Use NavKeys instead")
        const val LEGACY_MESSAGE_ID: String = "messageId"
    }
}

/**
 * Navigation key for opening a chat showing its messages.
 *
 * The "show messages" action is implicit, so callers only need to provide the
 * chat id. Prefer this over [ChatNavKey] with an explicit action string.
 *
 * @param chatId Chat ID to open.
 * @param openFromList When true, the chat list is placed beneath the chat room so pressing back
 * from the room returns to the list. Use when opening a chat outside the list context (e.g. from a
 * notification) where the list would otherwise not be on the back stack.
 */
@Serializable
@Parcelize
data class ShowChatMessagesNavKey(
    val chatId: Long,
    val openFromList: Boolean = false,
) : NoSessionNavKey.Optional, Parcelable

/**
 * Navigation key for Chat List
 *
 * @param createNewChat True if the Chat List screen should open with the Create New Chat flow
 * @param showMeetingTab True to open the Chat List with the Meetings tab selected
 */
@Serializable
@Parcelize
data class ChatListNavKey(
    val createNewChat: Boolean = false,
    val showMeetingTab: Boolean = false,
) : MainNavItemNavKey, Parcelable

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
    val nodeHandle: List<Long>,
) : NavKey {
    companion object Companion {
        const val KEY = "extra_contacts"
    }
}

/**
 * Navigation key for the contact info screen.
 *
 * @param email Email of the contact, or null when opening from a 1:1 chat.
 * @param chatId Id of the 1:1 chat with the contact, or null when opening by email.
 */
@Serializable
data class ContactInfoNavKey(
    val email: String? = null,
    val chatId: Long? = null,
) : NavKey {
    init {
        require(email != null || chatId != null) { "Either email or chatId must be provided" }
    }
}

/**
 * Navigation key for the node attachment history screen (NodeAttachmentHistoryActivity) showing
 * the files shared in a chat.
 *
 * @param chatId Id of the chat whose shared files are shown.
 */
@Serializable
data class NodeAttachmentHistoryNavKey(val chatId: Long) : NavKey

/**
 * Navigation key for the list of folders a contact shares with the user
 * (ContactFileListActivity).
 *
 * @param email Email of the contact.
 */
@Serializable
data class ContactSharedFoldersNavKey(val email: String) : NavKey

@Serializable
data class FileContactInfoNavKey(
    val folderHandle: Long,
    val folderName: String,
) : NavKey

@Serializable
data class TestPasswordNavKey(
    val wrongPasswordCounter: Int = 0,
    val isTestPasswordMode: Boolean = false,
    val isLogoutMode: Boolean = false,
) : NavKey

@Serializable
data class OfflineInfoNavKey(val handle: String) : NavKey

/**
 * Navigation key for the tags screen (TagsActivity) of the given node.
 *
 * @param nodeHandle The node handle whose tags are shown/edited.
 */
@Serializable
data class TagsNavKey(val nodeHandle: Long) : NavKey

/**
 * Navigation key for the version-history screen (VersionsFileActivity) of the given file.
 *
 * @param nodeHandle The file node handle whose versions are shown.
 */
@Serializable
data class VersionsFileNavKey(val nodeHandle: Long) : NavKey

@Serializable
data class LegacyFileInfoNavKey(val handle: Long) : NavKey

@Serializable
data class AuthenticityCredentialsNavKey(
    val email: String,
    val isIncomingShares: Boolean,
) : NavKey

@Serializable
@Parcelize
enum class SyncTab : Parcelable {
    FOLDERS,
    STALLED_ISSUES,
    SOLVED_ISSUES,
}

@Serializable
@Parcelize
data class SyncListNavKey(
    val initialTab: SyncTab = SyncTab.FOLDERS,
) : NavKey, Parcelable

@Serializable
data class SyncNewFolderNavKey(
    val syncType: SyncType = SyncType.TYPE_TWOWAY,
    val isFromDeviceCenter: Boolean = false,
    val remoteFolderHandle: Long? = null,
    val remoteFolderName: String? = null,
) : NavKey

@Serializable
data class SyncSelectStopBackupDestinationNavKey(
    val folderName: String? = null,
) : NavKey

@Serializable
data object SyncMegaPickerNavKey : NavKey

/**
 * Bottom sheet listing the resolutions available for a stalled sync issue.
 *
 * @param issueId id of the stalled issue. The issue itself is resolved from the shared
 * stalled issues state rather than carried whole, so the key stays serializable.
 */
@Serializable
data class SyncStalledIssueResolutionNavKey(val issueId: String) : NavKey

/**
 * Dialog asking whether a chosen resolution applies to one issue or to all of them.
 *
 * @param issueId id of the stalled issue being resolved
 * @param actionType name of the selected resolution action. The type is internal to the sync
 * feature, so it travels as its enum name and is resolved at the destination.
 */
@Serializable
data class SyncApplyToAllNavKey(
    val issueId: String,
    val actionType: String,
) : NavKey

@Serializable
@Parcelize
data class UpgradeAccountNavKey(
    val isUpgrade: Boolean = true,
    val isNewAccount: Boolean = false,
    val source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
) : NavKey, Parcelable

/**
 * Full-screen promo for the recommended discounted plan (DSN-3130 offer landing screen), opened
 * with a slide-up-from-bottom transition.
 *
 * @property source how the screen was opened; reported to analytics
 */
@Serializable
data class SubscriptionOfferNavKey(
    val source: SubscriptionOfferSource,
) : NavKey

/**
 * Downloading and streaming from a public link hit the transfer quota without a session, so the
 * warning is shown to anonymous users too; the screen then upsells plans and routes both of its
 * actions through login.
 */
@Serializable
@Parcelize
data class QuotaWarningUpgradeNavKey(
    val type: QuotaWarningType,
    val trigger: QuotaWarningTrigger,
) : NoSessionNavKey.Optional, Parcelable

@Serializable
@Parcelize
data class FileStorageNavKey(
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
data class AlbumImportPreviewNavKey(
    val photoId: Long,
    val albumLink: String? = null,
) : NoSessionNavKey.Optional

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
data class AlbumGetLinkNavKey(val albumId: Long) : NavKey

@Serializable
data class AlbumGetMultipleLinksNavKey(val albumIds: Set<Long>) : NavKey

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
    val nodeIds: List<Long>? = null,
    val isInShare: Boolean = false,
    val url: String? = null,
) : NoSessionNavKey.Optional

/**
 * Single NavKey for all text editor entry points (cloud node, local/zip file, chat attachment).
 * Branch in the destination by: chatId != null -> chat; localPath != null -> local file; else -> cloud node.
 */
@Serializable
data class LegacyTextEditorNavKey(
    val nodeHandle: Long? = null,
    val mode: String = TextEditorMode.View.value,
    val nodeSourceType: Int? = null,
    val fileName: String? = null,
    /** When true (e.g. new text file from Home), matches legacy home-page flag for uploads. */
    val fromHome: Boolean = false,
    val localPath: String? = null,
    val chatId: Long? = null,
    val messageId: Long? = null,
    /** Public file link URL; when set the editor resolves the node from this URL. */
    val publicUrl: String? = null,
    /** Serialized node string for public file links; used by the legacy TextEditorActivity. */
    val serializedNode: String? = null,
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
    val searchedItems: List<Long>? = null,
    val nodeHandles: List<Long>? = null,
    val mediaQueueTitle: String? = null,
    val collectionTitle: String? = null,
    val collectionId: Long? = null,
    val chatId: Long? = null,
    val msgId: Long? = null,
    val publicLinkUrl: String? = null,
    val localFilePath: String? = null,
) : NoSessionNavKey.Optional

@Serializable
data object VideoSectionNavKey : NavKey

@Serializable
data class MediaTimelinePhotoPreviewNavKey(
    val id: Long,
    val sortType: String,
    val filterType: String,
    val mediaSource: String,
    val anchorIndex: Int = 0,
    val totalCount: Int = 0,
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
data class LegacySettingsCameraUploadsActivityNavKey(
    val isShowHowToUploadPrompt: Boolean = false,
) : NavKey

/**
 * Navigation key for the Camera backup permissions screen, shown when the user attempts to
 * enable Camera uploads while the required media permissions have not been granted yet.
 */
@Serializable
data object CameraBackupPermissionsNavKey : NavKey

/**
 * Navigation key for GetLinkActivity that handles legacy navigation.
 *
 * @param handles List of node handles to get their links. If empty, the navigation destination will skip launching the Activity.
 */
@Serializable
data class GetLinkNavKey(
    val handles: List<Long> = emptyList(),
) : NavKey

@Serializable
data class LegacyVideoToPlaylistNavKey(
    val nodeHandle: Long,
) : NavKey {
    companion object {
        const val ADD_VIDEO_TO_PLAYLIST_RESULT = "ADD_VIDEO_TO_PLAYLIST_RESULT"
    }
}

@Serializable
data class ContactAttachmentNavKey(
    val chatId: Long,
    val messageId: Long,
) : NavKey

@Serializable
data object BusinessExpiredAlertNavKey : NavKey

@Serializable
data class LeftMeetingNavKey(
    val callEndedDueToFreePlanLimits: Boolean = false,
    val callEndedDueToTooManyParticipants: Boolean = false,
) : NoSessionNavKey.Optional

@Serializable
data class FileExplorerNavKey(
    val action: String,
    val shareUri: String? = null,
    val mimeType: String? = null,
) : NavKey {
    companion object {
        const val RESULT_FOLDER_HANDLE = "result_folder_handle"
    }
}

@Serializable
data object AudioSectionNavKey : NavKey
