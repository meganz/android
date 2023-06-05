package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get the OfflineNodeInformation for this node, the file and nested folders may or may not exist and may or may not be up to date
 * Some example paths returned:
 *  - file in drive root:       /data/user/0/mega.privacy.android.app/files/MEGA Offline/Welcome.pdf
 *  - sub-folder in drive:      /data/user/0/mega.privacy.android.app/files/MEGA Offline/folder1/folder2
 *  - file in backup:           /data/user/0/mega.privacy.android.app/files/MEGA Offline/in/Backups/MacBook Pro/My backup/Screenshot.png
 *  - file in incoming shared:  /data/user/0/mega.privacy.android.app/files/MEGA Offline/229921431902040/shared with friends/notes/hola.txt

 * @see [IsAvailableOfflineUseCase] to know if the node is available offline or not
 */
class GetOfflineNodeInformationUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val isNodeInInbox: IsNodeInInbox,
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke(node: Node): OfflineNodeInformation {
        var parents = nestedParentFolders(node)
        val isIncomingShareId = (
                node.takeIf { it.isIncomingShare }
                    ?: parents.firstOrNull()?.takeIf { it.isIncomingShare }
                )?.id
        val isInInbox = isNodeInInbox(node.id.longValue)
        if (isInInbox) {
            parents = parents.drop(1) //we don't need the root backup parent (Vault)
        }
        val path = joinPaths(
            *parents.map { it.name }.toTypedArray(),
        )

        return when {
            isInInbox -> {
                InboxOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                )
            }

            isIncomingShareId != null -> {
                IncomingShareOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                    incomingHandle = isIncomingShareId.longValue.toString()
                )
            }

            else ->
                OtherOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                )
        }

    }

    /**
     * returns an array with all nested parent folders of this node, from deepest to less deep.
     */
    private suspend fun nestedParentFolders(node: Node): List<Node> {
        val driveRootNode = nodeRepository.getRootNode()
        val nodes = ArrayList<Node>()
        var nodeToCheck = node
        while (true) {
            nodeToCheck = getParentNodeUseCase(nodeToCheck.id) ?: break
            if (nodeToCheck.id == driveRootNode?.id) {
                break
            }
            nodes.add(nodeToCheck)
        }
        return nodes.reversed()
    }

    private fun joinPaths(vararg paths: String?) =
        paths
            .filterNotNull()
            .filterNot { it == File.separator }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(
                separator = File.separator,
                prefix = File.separator,
                postfix = File.separator,
            ) {
                it.removePrefix(File.separator).removeSuffix(File.separator)
            } ?: File.separator
}