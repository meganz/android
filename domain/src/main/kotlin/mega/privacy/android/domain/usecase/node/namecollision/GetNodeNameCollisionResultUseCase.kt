package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import javax.inject.Inject

/**
 * Gets all the required info present of a node name collision
 */
class GetNodeNameCollisionResultUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invoke
     * @param nameCollision [NodeNameCollision]
     */
    suspend operator fun invoke(nameCollision: NameCollision): NodeNameCollisionResult {
        val collidedNode = getNodeByHandleUseCase(nameCollision.collisionHandle, true)
            ?: throw NodeDoesNotExistsException()

        val currentNodeThumbnail = if (nameCollision is NodeNameCollision && nameCollision.isFile)
            getThumbnailUseCase(
                nameCollision.nodeHandle
            )?.let { UriPath(it.absolutePath) } else null

        val collisionNodeThumbnail = if (collidedNode is FileNode) getThumbnailUseCase(
            collidedNode.id.longValue
        )?.let { UriPath(it.absolutePath) } else null

        val renameName = getNodeNameCollisionRenameNameUseCase(nameCollision)

        return with(collidedNode) {
            NodeNameCollisionResult(
                nameCollision = when (nameCollision) {
                    is NodeNameCollision.Default -> nameCollision.copy(renameName = renameName)

                    is NodeNameCollision.Chat -> nameCollision.copy(renameName = renameName)

                    is FileNameCollision -> nameCollision
                },
                collisionName = name,
                collisionSize = (this as? FileNode)?.size,
                collisionFolderContent = (this as? FolderNode)?.let {
                    nodeRepository.getFolderTreeInfo(it)
                },
                collisionLastModified = if (this is FileNode) modificationTime else creationTime,
                collisionThumbnail = collisionNodeThumbnail,
                thumbnail = currentNodeThumbnail,
                renameName = getNodeNameCollisionRenameNameUseCase(nameCollision)
            )
        }
    }
}