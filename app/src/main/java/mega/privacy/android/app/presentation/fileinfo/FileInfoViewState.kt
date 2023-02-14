package mega.privacy.android.app.presentation.fileinfo

/**
 * Represents the view state of the File info screen
 * @param oneOffViewEvent one-off events to be consumed by the view
 * @param jobInProgressState indicates if there are any job in progress that needs to be notified
 * @param historyVersions the num of history versions that this file contains,
 * 0 if it's not a file or doesn't contain history versions
 * @param isNodeInInbox determines if the node is in the inbox or not (aka backup)
 * @param isNodeInRubbish determines if the node is in the rubbish bin or not
 * @param previewUriString the uri of the file containing the preview
 * @param thumbnailUriString the uri of the file containing the thumbnail, just as a fallback in case there's no [previewUriString]
 */
data class FileInfoViewState(
    val oneOffViewEvent: FileInfoOneOffViewEvent? = null,
    val jobInProgressState: FileInfoJobInProgressState? = FileInfoJobInProgressState.InitialLoading,
    val historyVersions: Int = 0,
    val isNodeInInbox: Boolean = false,
    val isNodeInRubbish: Boolean = false,
    val previewUriString: String? = null,
    val thumbnailUriString: String? = null,
) {
    /**
     * determines if the file history versions should be shown
     */
    val showHistoryVersions = !isNodeInInbox && historyVersions > 0

    /**
     * to get the uri to use for the preview, it uses [previewUriString] and [thumbnailUriString] only as fallback
     */
    val actualPreviewUriString = previewUriString ?: thumbnailUriString
}