package mega.privacy.android.app.zippreview.ui

import mega.privacy.android.app.zippreview.domain.FileType

/**
 * The UI object of zip info
 * @param name name for display
 * @param path current zip entry path
 * @param parent parent for back preview directory
 * @param folderInfo display the file and folder number of next directory of current folder
 * @param imageResourceId imageResourceId for display
 * @param fileType fileType
 */
data class ZipInfoUIO(
    val name: String,
    val path: String,
    val parent: String?,
    val folderInfo: String?,
    val imageResourceId: Int,
    val fileType: FileType
)
