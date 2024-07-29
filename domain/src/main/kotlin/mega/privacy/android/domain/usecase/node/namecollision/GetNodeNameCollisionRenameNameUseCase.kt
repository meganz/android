package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject


/**
 * Gets the name for rename a collision item in case the user wants to rename it.
 * Before returning the new name, always check if there is another collision with it.
 */
class GetNodeNameCollisionRenameNameUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
) {

    /**
     * Invoke
     * @param nameCollision [NodeNameCollision]
     */
    suspend operator fun invoke(nameCollision: NameCollision): String {
        val parentNode = when (nameCollision.parentHandle) {
            -1L -> getRootNodeUseCase()
            else -> getNodeByHandleUseCase(nameCollision.parentHandle)
        } ?: throw NodeDoesNotExistsException()
        return generateSequence(nameCollision.name) {
            it.getPossibleRenameName()
        }.first { newName ->
            getChildNodeUseCase(parentNode.id, newName) == null
        }
    }
}

/**
 * Gets a possible name for rename a collision item in case the user wants to rename it.
 *
 * @return The rename name.
 */
internal fun String.getPossibleRenameName(): String {
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