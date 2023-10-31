package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Is available offline
 */
class IsAvailableOfflineUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param node [TypedNode]
     * @return true if the file is available offline and up to date
     */
    suspend operator fun invoke(node: TypedNode): Boolean {
        val offlineNodeInformation =
            nodeRepository.getOfflineNodeInformation(node.id) ?: return false
        return offlineNodeInformation.isFolder || fileUpToDate(node, offlineNodeInformation)
    }

    private fun fileUpToDate(
        node: TypedNode,
        offlineNodeInformation: OfflineNodeInformation,
    ) = (offlineNodeInformation.lastModifiedTime ?: 0L) >= (node as FileNode).modificationTime
}