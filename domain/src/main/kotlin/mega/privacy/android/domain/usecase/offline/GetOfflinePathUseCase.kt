package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get the path where this node should be saved when saved offline, the file and nested folders may or may not exist and may or may not be up to date
 * Some examples:
 *  - file in drive root:       /data/user/0/mega.privacy.android.app/files/MEGA Offline/Welcome.pdf
 *  - sub-folder in drive:      /data/user/0/mega.privacy.android.app/files/MEGA Offline/folder1/folder2
 *  - file in backup:           /data/user/0/mega.privacy.android.app/files/MEGA Offline/in/Backups/MacBook Pro/My backup/Screenshot.png
 *  - file in incoming shared:  /data/user/0/mega.privacy.android.app/files/MEGA Offline/229921431902040/shared with friends/notes/hola.txt

 * @see [IsAvailableOfflineUseCase] to know if the node is available offline or not
 */
class GetOfflinePathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val nodeRepository: NodeRepository,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val isNodeInInbox: IsNodeInInbox,
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke(node: Node): String {
        val offlineDeviceFolder = when {
            isNodeInInbox(node.id.longValue) -> fileSystemRepository.getOfflineInboxPath()
            else -> fileSystemRepository.getOfflinePath()
        }
        return joinPaths(
            offlineDeviceFolder,
            *folderArrayForOfflinePath(node),
            node.name
        )
    }

    /**
     * gets the [nestedParentFolders] and convert it to the proper format for offline path:
     * - remove root node in case of drive node or backup node
     * - adds root id in case of an incoming shared node
     */
    private suspend fun folderArrayForOfflinePath(node: Node): Array<String> {
        val backupRootNodeId = nodeRepository.getBackupFolderId()
        val driveRootNode = nodeRepository.getRootNode()
        val nodes = nestedParentFolders(node)
        val names = nodes.map { it.name }
        return when {
            nodes.isEmpty() -> names
            nodes.first().id == driveRootNode?.id || nodes.getOrNull(1)?.id == backupRootNodeId -> {
                names.drop(1)
            }

            nodes.first().isIncomingShare -> {
                names.toMutableList().also { it.add(0, nodes.first().id.longValue.toString()) }
            }

            else -> names
        }.toTypedArray()
    }

    /**
     * returns an array with all nested parent folders of this node, from deepest to less deep.
     */
    private suspend fun nestedParentFolders(node: Node): List<Node> {
        val nodes = ArrayList<Node>()
        var nodeToCheck = node
        while (true) {
            nodeToCheck = getParentNodeUseCase(nodeToCheck.id) ?: break
            nodes.add(nodeToCheck)
        }
        return nodes.reversed()
    }

    private fun joinPaths(vararg paths: String?) =
        paths
            .filterNotNull()
            .filterNot { it == File.separator }
            .joinToString(
                separator = File.separator,
                prefix = File.separator,
            ) {
                it.removePrefix(File.separator).removeSuffix(File.separator)
            }
}