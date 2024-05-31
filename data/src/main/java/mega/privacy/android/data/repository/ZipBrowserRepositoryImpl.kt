package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.zipbrowser.ZipTreeNodeMapper
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ZipBrowserRepository
import java.io.File
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
                                parentPath = subParentPath
                            )

                            zipNodeTree[subPath] = zipTreeNode

                            // If parent path is not empty add current path to map
                            // Empty path represents root directory
                            if (!subParentPath.isNullOrEmpty()) {
                                val parentNode = zipNodeTree[subParentPath] ?: continue
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
}