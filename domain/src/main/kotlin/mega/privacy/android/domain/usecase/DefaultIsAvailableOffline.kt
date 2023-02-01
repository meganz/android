package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class DefaultIsAvailableOffline @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getOfflineFile: GetOfflineFile,
) : IsAvailableOffline {
    override suspend fun invoke(node: TypedNode): Boolean {
        val nodeInformation = nodeRepository.getOfflineNodeInformation(node.id) ?: return false
        val offlineFolder = getOfflineFile(nodeInformation).takeIf { it.exists() } ?: return false
        return node is FolderNode || localFileIsUpToDate(offlineFolder, node)
    }

    private fun localFileIsUpToDate(
        offlineFile: File,
        node: TypedNode,
    ) = offlineFile.lastModified()
        .milliseconds
        .inWholeSeconds >= (node as FileNode).modificationTime
}