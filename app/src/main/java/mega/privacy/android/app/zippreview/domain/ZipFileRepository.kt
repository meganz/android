package mega.privacy.android.app.zippreview.domain

import java.util.zip.ZipFile

/**
 * Zip repository to handle zip file
 */
interface ZipFileRepository {

    /**
     * Unpack Zip file
     * @param zipFullPath Zip file full path
     * @param unzipRootPath the unpacked root path
     * @return true is unpack succeed.
     */
    suspend fun unzipFile(zipFullPath: String, unzipRootPath: String): Boolean

    /**
     * Init the ZipTreeNode. Created ZipTreeMap using zip entries of current zip file
     * @param zipFile
     */
    suspend fun initZipTreeNode(zipFile: ZipFile)

    /**
     * Updated zip info list when the directory changed.
     * @param folderPath current zip folder path
     * @param zipFile zip file
     * @return the list in current content
     */
    fun updateZipInfoList(zipFile: ZipFile, folderPath: String): List<ZipTreeNode>

    /**
     * Get the zip info list of current folder's parent
     * @param folderPath current folder path
     * @param isEmptyFolder current folder whether is empty
     * @return ZipTreeNode List
     */
    fun getParentZipInfoList(folderPath: String, isEmptyFolder: Boolean): List<ZipTreeNode>
}