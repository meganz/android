package mega.privacy.android.app.presentation.fileinfo

/**
 * Represents the view state of the File info screen
 * @param historyVersions the num of history versions that this file contains,
 * 0 if it's not a file or doesn't contain history versions
 */
data class FileInfoViewState(
    val historyVersions: Int = 0,
)