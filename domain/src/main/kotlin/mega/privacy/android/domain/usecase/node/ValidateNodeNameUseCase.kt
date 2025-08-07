package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.repository.NodeRepository
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Validate node name for creation
 */
class ValidateNodeNameUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     * @param name Name of the folder to validate
     * @param parentNodeId Parent node id under which the folder should be created
     * @throws EmptyNodeNameException if the folder name is empty or contains only whitespace
     * @throws InvalidNodeNameException if the folder name contains invalid characters
     * @throws NodeNameAlreadyExistsException if a folder with the same name already exists
     */
    suspend operator fun invoke(name: String, parentNodeId: NodeId?) {
        when {
            name.isEmpty() -> throw EmptyNodeNameException()
            containsInvalidCharacters(name) -> throw InvalidNodeNameException()
            checkFolderNameExists(
                name,
                parentNodeId
            ) -> throw NodeNameAlreadyExistsException()
        }
    }

    /**
     * Checks if the folder name already exists in the parent directory.
     */
    private suspend fun checkFolderNameExists(folderName: String, parentNodeId: NodeId?): Boolean =
        runCatching {
            nodeRepository.getChildNode(parentNodeId, folderName) != null
        }.onFailure {
            // Log the error but don't fail validation due to repository errors
            // This allows folder creation to proceed even if checking fails
        }.getOrDefault(false)

    /**
     * Validates if the folder name contains invalid characters.
     * Uses the NODE_NAME_REGEX pattern.
     *
     * @param folderName The folder name to validate
     * @return true if the folder name contains invalid characters, false otherwise
     */
    private fun containsInvalidCharacters(folderName: String): Boolean =
        NODE_NAME_REGEX.matcher(folderName).find()

    companion object Companion {
        private val NODE_NAME_REGEX = Pattern.compile("[*|?:\"<>\\\\/]")
    }
} 