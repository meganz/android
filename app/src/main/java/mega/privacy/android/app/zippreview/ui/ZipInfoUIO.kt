package mega.privacy.android.app.zippreview.ui

import mega.privacy.android.app.zippreview.domain.FileType

/**
 * The UI object of zip info
 */
data class ZipInfoUIO(
    val zipFileName: String,
    val folderInfo: String?,
    val imageResourceId: Int,
    val displayedFileName: String,
    val fileType: FileType
)
