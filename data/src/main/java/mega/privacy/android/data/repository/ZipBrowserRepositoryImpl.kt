package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.zipbrowser.ZipTreeNodeMapper
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ZipBrowserRepository
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Zip repository implementation class
 */
class ZipBrowserRepositoryImpl @Inject constructor(
    private val zipTreeNodeMapper: ZipTreeNodeMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ZipBrowserRepository {

    override suspend fun getZipNodeTree(zipFile: ZipFile?): Map<String, ZipTreeNode> =
        withContext(ioDispatcher) {
            val zipNodeTree = mutableMapOf<String, ZipTreeNode>()
            zipFile?.entries()?.toList()?.forEach { zipEntry ->
                zipEntry.name.let { name ->
                    val nodeDepth = name.getZipTreeNodeDepth()
                    for (i in 1..nodeDepth) {
                        //Get every sub path of current zip entry. For example, the path zip entry
                        // path is 1/2/3.txt, the sub paths respectively are 1 1/2 1/2/3.txt
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
                        var zipTreeNode = zipNodeTree[subPath]

                        // If node doesn't exist, create one, otherwise ignore it
                        if (zipTreeNode == null) {
                            zipTreeNode = zipTreeNodeMapper(
                                zipEntry = zipEntry,
                                name = subName,
                                path = subPath,
                                parentPath = subParentPath,
                                zipEntryType = if (i == nodeDepth) {
                                    when {
                                        zipEntry.isDirectory -> ZipEntryType.Folder
                                        name.endsWith(SUFFIX_ZIP) -> ZipEntryType.Zip
                                        else -> ZipEntryType.File
                                    }
                                } else {
                                    ZipEntryType.Folder
                                }
                            )

                            zipNodeTree[subPath] = zipTreeNode

                            // If parent path is not empty add current path to map
                            // Empty path represents root directory
                            if (!subParentPath.isNullOrEmpty()) {
                                zipNodeTree[subParentPath]?.let { parentNode ->
                                    val updatedChildren =
                                        parentNode.children.toMutableList().apply {
                                            add(zipTreeNode)
                                        }
                                    val updatedParentNode =
                                        parentNode.copy(children = updatedChildren)
                                    zipNodeTree[subParentPath] = updatedParentNode
                                }
                            }
                        }
                    }
                }
            }
            zipNodeTree
        }

    /**
     * Get zip tree node name for init zip tree map
     * @return zip tree name
     */
    private fun String.getZipTreeNodeName() =
        removeSuffix(File.separator).split(File.separator).last()

    /**
     * Get sub path for init zip tree map
     * @param depth depth of sub path
     * @return sub path
     */
    private fun String.getSubPathByDepth(depth: Int) =
        removeSuffix(File.separator).split(File.separator).take(depth).joinToString(File.separator)

    /**
     * Get current zip tree node depth for init zip tree map
     * @return zip tree node depth
     */
    private fun String.getZipTreeNodeDepth() =
        removeSuffix(File.separator).split(File.separator).size

    override suspend fun unzipFile(zipFile: ZipFile, unzipRootPath: String): Boolean =
        withContext(ioDispatcher) {
            runCatching {
                val zipEntries = zipFile.entries()
                zipEntries.toList().forEach { zipEntry ->
                    val zipDestination = File(unzipRootPath + zipEntry.name)
                    val canonicalPath = zipDestination.canonicalPath
                    if (canonicalPath.startsWith(unzipRootPath)) {
                        if (zipEntry.isDirectory) {
                            if (!zipDestination.exists()) {
                                zipDestination.mkdirs()
                            }
                        } else {
                            val inputStream = zipFile.getInputStream(zipEntry)
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
                        }
                    } else {
                        throw SecurityException()
                    }
                }
                true
            }.recover { e ->
                Timber.e(e)
                false
            }.getOrNull() ?: false
        }

    companion object {
        private const val SUFFIX_ZIP = ".zip"
    }
}