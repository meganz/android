package mega.privacy.android.app.zippreview.domain

import java.util.zip.ZipEntry

data class ZipInfoBO(
    val zipEntry: ZipEntry,
    val zipFileName: String,
    val fileType: ZipFileType,
    val info: String
)

enum class ZipFileType {
    FOLDER, ZIP, FILE, UNKNOWN
}
