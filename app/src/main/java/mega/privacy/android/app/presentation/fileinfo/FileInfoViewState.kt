package mega.privacy.android.app.presentation.fileinfo

import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.FolderTreeInfo

/**
 * Represents the view state of the File info screen
 * @param oneOffViewEvent one-off events to be consumed by the view
 * @param jobInProgressState indicates if there are any job in progress that needs to be notified
 * @param historyVersions the num of history versions that this file contains, 0 if it's not a file or doesn't contain history versions
 * @param isNodeInInbox determines if the node is in the inbox or not (aka backup)
 * @param isNodeInRubbish determines if the node is in the rubbish bin or not
 * @param previewUriString the uri of the file containing the preview
 * @param thumbnailUriString the uri of the file containing the thumbnail, just as a fallback in case there's no [previewUriString]
 * @param folderTreeInfo the folder info if the node is a folder
 */
data class FileInfoViewState(
    val oneOffViewEvent: FileInfoOneOffViewEvent? = null,
    val jobInProgressState: FileInfoJobInProgressState? = FileInfoJobInProgressState.InitialLoading,
    val historyVersions: Int = 0,
    val isNodeInInbox: Boolean = false,
    val isNodeInRubbish: Boolean = false,
    val previewUriString: String? = null,
    val thumbnailUriString: String? = null,
    val folderTreeInfo: FolderTreeInfo? = null,
) {
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
    val actualPreviewUriString = (previewUriString ?: thumbnailUriString)?.let {
        if (it.startsWith("/")) "file:$it" else it
    }

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
}
