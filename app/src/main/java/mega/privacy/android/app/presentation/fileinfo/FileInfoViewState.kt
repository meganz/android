package mega.privacy.android.app.presentation.fileinfo

import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare

/**
 * Represents the view state of the File info screen
 * @param typedNode for this screen
 * @param oneOffViewEvent one-off events to be consumed by the view
 * @param jobInProgressState indicates if there are any job in progress that needs to be notified
 * @param historyVersions the num of history versions that this file contains, 0 if it's not a file or doesn't contain history versions
 * @param isNodeInInbox determines if the node is in the inbox or not (aka backup)
 * @param isNodeInRubbish determines if the node is in the rubbish bin or not
 * @param previewUriString the uri of the file containing the preview
 * @param thumbnailUriString the uri of the file containing the thumbnail, just as a fallback in case there's no [previewUriString]
 * @param folderTreeInfo the folder info if the node is a folder
 * @param isShareContactExpanded outShares are shown if is set to true
 * @param outShares shares in case of a node shared with others as outShare
 * @param nodeLocationInfo the location info of the node
 * @param isAvailableOffline true if the file is available offline
 * @param isAvailableOfflineEnabled true if offline availability can be changed, false if not
 * @param inShareOwnerContactItem the [ContactItem] of the owner
 * @param accessPermission the [AccessPermission] the user has to this node
 * @param outShareContactShowOptions the contact selected to show related options
 * @param outShareContactsSelected a list of contacts selected to batch modify
 */
data class FileInfoViewState(
    val typedNode: TypedNode? = null,
    val oneOffViewEvent: FileInfoOneOffViewEvent? = null,
    val jobInProgressState: FileInfoJobInProgressState? = FileInfoJobInProgressState.InitialLoading,
    val historyVersions: Int = 0,
    val isNodeInInbox: Boolean = false,
    val isNodeInRubbish: Boolean = false,
    val previewUriString: String? = null,
    val thumbnailUriString: String? = null,
    val folderTreeInfo: FolderTreeInfo? = null,
    val isShareContactExpanded: Boolean = false,
    val outShares: List<MegaShare> = emptyList(),
    val nodeLocationInfo: LocationInfo? = null,
    val isAvailableOffline: Boolean = false,
    val isAvailableOfflineEnabled: Boolean = false,
    val inShareOwnerContactItem: ContactItem? = null,
    val accessPermission: AccessPermission = AccessPermission.UNKNOWN,
    val outShareContactShowOptions: MegaShare? = null,
    val outShareContactsSelected: List<String> = emptyList(),
) {

    /**
     * title for this screen, node's name
     */
    val title: String = typedNode?.name.orEmpty()

    /**
     * determines if the file history versions should be shown
     */
    val showHistoryVersions = !isNodeInInbox && historyVersions > 0

    /**
     * determines if the folder history versions should be shown
     */
    val showFolderHistoryVersions = !isNodeInInbox && (folderTreeInfo?.numberOfVersions ?: 0) > 0

    /**
     * to get the uri to use for the preview, it gets [previewUriString] and [thumbnailUriString] as fallback
     */
    val actualPreviewUriString = previewUriString ?: thumbnailUriString

    /**
     * computed utility field to get the folder's previous versions size string
     */
    val folderVersionsSizeInBytesString: String by lazy {
        Util.getSizeString(folderTreeInfo?.sizeOfPreviousVersionsInBytes ?: 0)
    }

    /**
     * computed utility field to get the folder's current version size string
     */
    val folderCurrentVersionSizeInBytesString: String by lazy {
        Util.getSizeString(folderTreeInfo?.totalCurrentSizeInBytes ?: 0)
    }

    /**
     * computed utility field to get the folder content string. Ex "2 folders . 5 files"
     */
    val folderContentInfoString by lazy {
        folderTreeInfo?.let {
            TextUtil.getFolderInfo(
                it.numberOfFolders - 1, //-1 because we don't want to count the folder itself
                it.numberOfFiles
            )
        } ?: ""
    }

    /**
     * computed utility field to get the folder content total size, including current and previous versions.
     */
    val sizeString by lazy {
        folderTreeInfo?.let {
            Util.getSizeString(it.totalCurrentSizeInBytes + it.sizeOfPreviousVersionsInBytes)
        } //file size should be also returned
    }

    /**
     * the limited amount of outshares to be shown
     */
    val outSharesCoerceMax by lazy {
        outShares.takeIf { it.size <= MAX_NUMBER_OF_CONTACTS_IN_LIST }
            ?: (outShares.take(MAX_NUMBER_OF_CONTACTS_IN_LIST))
    }

    /**
     * true in case [outSharesCoerceMax] are not representing all the outShares
     */
    val thereAreMoreOutShares = outShares.size > MAX_NUMBER_OF_CONTACTS_IN_LIST

    /**
     * the amount of outShares that are not represented by [outSharesCoerceMax]
     */
    val extraOutShares = (outShares.size - MAX_NUMBER_OF_CONTACTS_IN_LIST).coerceAtLeast(0)

    /**
     * true if this node is an incoming shared root node
     */
    val isIncomingSharedNode = inShareOwnerContactItem != null

    /**
     * label for the owner of this node in case it's an incoming shared node
     */
    val ownerLabel by lazy {
        inShareOwnerContactItem?.contactData?.alias
            ?: inShareOwnerContactItem?.contactData?.fullName
            ?: inShareOwnerContactItem?.email
    }

    companion object {
        internal const val MAX_NUMBER_OF_CONTACTS_IN_LIST = 5
    }
}
