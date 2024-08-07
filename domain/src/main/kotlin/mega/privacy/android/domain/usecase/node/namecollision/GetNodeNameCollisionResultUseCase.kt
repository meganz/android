package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
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
) {

    /**
     * Invoke
     * @param nameCollision [NodeNameCollision]
     */
    suspend operator fun invoke(nameCollision: NameCollision): NodeNameCollisionResult {
        val collidedNode = getNodeByHandleUseCase(nameCollision.collisionHandle, true)
            ?: throw NodeDoesNotExistsException()

        val currentNodeThumbnail = runCatching {
            if (nameCollision is NodeNameCollision && nameCollision.isFile) getThumbnailUseCase(
                nameCollision.nodeHandle
            )?.let { UriPath(it.toURI().toString()) } else null
        }.getOrNull()

        val collisionNodeThumbnail = runCatching {
            if (collidedNode is FileNode) getThumbnailUseCase(
                collidedNode.id.longValue
            )?.let { UriPath(it.toURI().toString()) } else null
        }.getOrNull()

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
                    FolderTreeInfo(
                        numberOfFolders = it.childFolderCount,
                        numberOfFiles = it.childFileCount,
                        numberOfVersions = it.versionCount,
                        totalCurrentSizeInBytes = 0,
                        sizeOfPreviousVersionsInBytes = 0
                    )
                },
                collisionLastModified = if (this is FileNode) modificationTime else creationTime,
                collisionThumbnail = collisionNodeThumbnail,
                thumbnail = currentNodeThumbnail,
                renameName = getNodeNameCollisionRenameNameUseCase(nameCollision)
            )
        }
    }
}