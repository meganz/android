package mega.privacy.android.app.zippreview.ui

import mega.privacy.android.app.zippreview.domain.ZipFileType

data class ZipInfoUIO(
    val zipFileName: String,
    val folderInfo: String?,
    val imageResourceId: Int,
    val displayedFileName: String,
    val fileType: ZipFileType
)
