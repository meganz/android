package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import java.util.zip.ZipFile

/**
 * Zip browser repository
 */
interface ZipBrowserRepository {

    /**
     * Get ZipNodeTree
     *
     * @param zipFile ZipFile
     * @return ZipNodeTree Map<String, ZipTreeNode>
     *
     * The ZipNodeTree contains all zip tree nodes converted from zip entries.
     * It represents the complete structure of the zip file, is used for the
     * display and manipulation of folders/files at every level
     *
     * The key represents the paths of every level folder/file, while value represents
     * the ZipTreeNode converted from each level folder or file.
     *
     * For example,
     * the zip file includes a zip file call file.zip the path is folder/SubFolder/file.zip
     *
     * The zip entries are
     * folder/,
     * folder/SubFolder,
     * folder/SubFolder/file.zip
     *
     * The ZipNodeTree is
     * [folder, ZipTreeNode],
     * [Folder/SubFolder, ZipTreeNode],
     * [older/SubFolder/file.zip, ZipTreeNode]
     */
    suspend fun getZipNodeTree(zipFile: ZipFile?): Map<String, ZipTreeNode>

    /**
     * Unzip file
     * @param zipFile ZipFile
     * @param unzipRootPath unzip destination path
     * @return true is unzip succeed.
     */
    suspend fun unzipFile(zipFile: ZipFile, unzipRootPath: String): Boolean
}