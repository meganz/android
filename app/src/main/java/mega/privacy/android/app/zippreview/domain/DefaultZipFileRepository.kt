package mega.privacy.android.app.zippreview.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.IoDispatcher
import java.io.*
import java.lang.Exception
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Zip repository implementation class
 */
class DefaultZipFileRepository @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    ZipFileRepository {
    companion object {
        private const val SUFFIX_ZIP = ".zip"
    }

    private val zipTreeNodeMap: ZipTreeMap = ZipTreeMap()

    override suspend fun unzipFile(zipFullPath: String, unzipRootPath: String): Boolean {
        return withContext(ioDispatcher) {
            unzip(zipFullPath, unzipRootPath)
        }
    }

    /**
     * Unzip file
     * @param zipFullPath zip file path
     * @param unzipRootPath unzip destination path
     * @return true is unzip succeed.
     */
    private fun unzip(zipFullPath: String, unzipRootPath: String): Boolean {
        val zipFile = ZipFile(zipFullPath)
        val zipEntries = zipFile.entries()
        zipEntries.toList().forEach {
            val zipDestination = File(unzipRootPath + it.name)
            if (it.isDirectory) {
                if (!zipDestination.exists()) {
                    zipDestination.mkdirs()
                }
            } else {
                try {
                    val inputStream = zipFile.getInputStream(it)
                    //Get the parent file. If it is null or
                    // doesn't exist, created the parent folder.
                    val parentFile = zipDestination.parentFile
                    if (parentFile != null) {
                        if (!parentFile.exists()) {
                            parentFile.mkdirs()
                        }
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        FileOutputStream(zipDestination).use { outputStream ->
                            //Write the file.
                            while (inputStream.read(buffer)
                                    .also { readCount -> count = readCount } != -1
                            ) {
                                byteArrayOutputStream.write(buffer, 0, count)
                                val bytes = byteArrayOutputStream.toByteArray()
                                outputStream.write(bytes)
                                byteArrayOutputStream.reset()
                            }
                        }
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }

        }
        return true
    }

    override fun getParentZipInfoList(
        folderPath: String,
        isEmptyFolder: Boolean
    ): List<ZipTreeNode> {
        val parentNodeKey = if (isEmptyFolder) {
            //If folder is empty, using saved folder path to back preview directory
            zipTreeNodeMap[folderPath.removeSuffix("/")]?.path
        } else {
            //If folder is not empty, using parent path of sub item to back preview directory
            zipTreeNodeMap[folderPath.removeSuffix("/")]?.parent
        }
        // No at root level, show parent and it's siblings
        return zipTreeNodeMap[parentNodeKey]?.let { parentNode ->
            parentNode.parent?.let { grandpaPath ->
                zipTreeNodeMap[grandpaPath]?.children ?: mutableListOf()
                // Parent's parent is null, should show root children
            } ?: getRootChildren()
            // Parent is null, return empty list
        } ?: mutableListOf()
    }

    override fun updateZipInfoList(zipFile: ZipFile, folderPath: String): List<ZipTreeNode> {
        return if (folderPath.isNotEmpty()) {
            zipTreeNodeMap[folderPath.removeSuffix("/")]?.children ?: mutableListOf()
        } else {
            //If folder is empty, show the root directory
            getRootChildren()
        }
    }

    /**
     * Get items of root directory
     * @return List<ZipTreeNode>
     */
    private fun getRootChildren() = zipTreeNodeMap.values.filter {
        it.parent == null
    }

    /**
     * Using zip tree map could save all of zip entries information and could created complete
     * directory structure to handle the switch on different zip file directory. Meanwhile, avoid
     * repeatedly iterate zip entries when the directory is changed.
     */
    override suspend fun initZipTreeNode(zipFile: ZipFile) {
        withContext(ioDispatcher) {
            zipFile.entries().toList().forEach { zipEntry ->
                zipEntry.name.let { name ->
                    val nodeDepth = name.getZipTreeNodeDepth()
                    for (i in 1..nodeDepth) {
                        //Get every sub path of current zip entry. For example, the path zip entry
                        // path is 1/2/3.txt, the sub paths respectively are 1/ 1/2/ 1/2/3.txt
                        val subPath = name.getSubPathByDepth(i)
                        //Get name of current sub path
                        val subName = subPath.getZipTreeNodeName()
                        //Get parent path of current sub path. For example, if current sub path is 1/2/
                        //its parent path is 1/
                        val subParentPath = if (i == 1) {
                            null
                        } else {
                            name.getSubPathByDepth(i - 1)
                        }
                        //Get current zip tree node using sub path
                        var zipTreeNode = zipTreeNodeMap[subPath]

                        // If node doesn't exist, create one, otherwise ignore it
                        if (zipTreeNode == null) {
                            zipTreeNode = ZipTreeNode(
                                name = subName,
                                path = subPath,
                                size = zipEntry.size,
                                fileType = if (i == nodeDepth) {
                                    if (zipEntry.isDirectory) {
                                        FileType.FOLDER
                                    } else {
                                        when {
                                            subPath.endsWith(SUFFIX_ZIP) -> FileType.ZIP
                                            else -> FileType.FILE
                                        }
                                    }
                                } else {
                                    FileType.FOLDER
                                },
                                parent = subParentPath,
                                children = mutableListOf(),
                            )
                            zipTreeNodeMap[subPath] = zipTreeNode

                            // If parent path is not empty add current path to map
                            // Empty path represents root directory
                            if (!subParentPath.isNullOrEmpty()) {
                                val parentNode = zipTreeNodeMap[subParentPath]
                                parentNode?.children?.add(zipTreeNode)
                            }
                        }
                    }
                }
            }
        }
    }
}