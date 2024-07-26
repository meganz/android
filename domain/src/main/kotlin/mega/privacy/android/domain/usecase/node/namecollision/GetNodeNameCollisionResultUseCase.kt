package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import javax.inject.Inject

/**
 * Gets all the required info present of a node name collision
 */
class GetNodeNameCollisionResultUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
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

        return with(collidedNode) {
            NodeNameCollisionResult(
                nameCollision = nameCollision,
                collisionName = name,
                collisionSize = (this as? FileNode)?.size,
                collisionFolderContent = (this as? FolderNode)?.let {
                    nodeRepository.getFolderTreeInfo(it)
                },
                collisionLastModified = if (this is FileNode) modificationTime else creationTime,
                collisionThumbnail = collisionNodeThumbnail,
                thumbnail = currentNodeThumbnail,
                renameName = getRenameName(nameCollision)
            )
        }
    }


    /**
     * Gets the name for rename a collision item in case the user wants to rename it.
     * Before returning the new name, always check if there is another collision with it.
     *
     * @param collision [NameCollision] from which the rename name has to be get.
     * @return Single with the rename name.
     */
    private suspend fun getRenameName(collision: NameCollision): String {
        val parentNode = if (collision.parentHandle == -1L)
            getRootNodeUseCase()
        else
            getNodeByHandleUseCase(collision.parentHandle)
        if (parentNode == null) throw NodeDoesNotExistsException()

        var newName = collision.name
        var newCollision: UnTypedNode?
        do {
            newName = newName.getPossibleRenameName()
            newCollision = getChildNodeUseCase(parentNode.id, newName)
        } while (newCollision != null)
        return newName
    }

    /**
     * Gets a possible name for rename a collision item in case the user wants to rename it.
     *
     * @return The rename name.
     */
    private fun String.getPossibleRenameName(): String {
        var extension = substringAfterLast('.', "")
        val pointIndex = if (extension.isEmpty())
            length
        else
            (lastIndexOf(extension) - 1).coerceAtLeast(0)
        val name = substring(0, pointIndex)
        extension = substring(pointIndex, length)
        val pattern = "\\(\\d+\\)".toRegex()
        val matches = pattern.findAll(name)

        val renameName = when {
            matches.count() > 0 -> {
                val result = matches.last().value
                val number = result.replace("(", "").replace(")", "")
                val newNumber = number.toInt() + 1
                val firstIndex = lastIndexOf('(')
                name.substring(0, firstIndex + 1).plus("$newNumber)")
            }

            else -> name.plus(" (1)")
        }

        return renameName.plus(extension)
    }
}