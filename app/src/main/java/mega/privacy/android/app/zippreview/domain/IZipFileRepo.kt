package mega.privacy.android.app.zippreview.domain

import java.util.zip.ZipFile

/**
 * Zip repository to handle zip file
 */
interface IZipFileRepo {

    /**
     * Unpack Zip file
     * @param zipFullPath Zip file full path
     * @param unZipRootPath the unpacked root path
     * @return true is unpack succeed.
     */
    suspend fun unzipFile(zipFullPath: String, unZipRootPath: String): Boolean

    /**
     * Updated zip info list when the directory changed.
     * @param folderPath current zip folder path
     * @param zipFile zip file
     * @return the list in current content
     */
    suspend fun updateZipInfoList(
        unknownStr: String,
        zipFile: ZipFile,
        folderPath: String
    ): List<ZipInfoBO>
}