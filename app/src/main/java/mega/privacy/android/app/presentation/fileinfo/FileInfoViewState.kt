package mega.privacy.android.app.presentation.fileinfo

/**
 * Represents the view state of the File info screen
 * @param historyVersions the num of history versions that this file contains,
 * 0 if it's not a file or doesn't contain history versions
 * @param isNodeInInbox determines if the node is in the inbox or not (aka backup)
 */
data class FileInfoViewState(
    val historyVersions: Int = 0,
    val isNodeInInbox: Boolean = false,
) {
    /**
     * determines if the file history versions should be shown
     */
    val showHistoryVersions = !isNodeInInbox && historyVersions > 0
}