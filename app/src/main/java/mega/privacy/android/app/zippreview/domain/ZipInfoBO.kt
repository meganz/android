package mega.privacy.android.app.zippreview.domain

import java.util.zip.ZipEntry

/**
 * Business object of zip info
 */
data class ZipInfoBO(
    val zipEntry: ZipEntry,
    val zipFileName: String,
    val fileType: FileType,
    val info: String
)

/**
 * File type
 */
enum class FileType {
    FOLDER, ZIP, FILE, UNKNOWN
}
