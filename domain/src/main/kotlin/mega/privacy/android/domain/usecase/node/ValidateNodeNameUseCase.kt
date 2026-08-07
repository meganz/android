package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Validate node name for creation
 */
class ValidateNodeNameUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val validateNodeNameCharactersUseCase: ValidateNodeNameCharactersUseCase,
) {

    /**
     * invoke
     * @param name Name of the folder to validate
     * @param parentNodeId Parent node id under which the folder should be created
     * @throws EmptyNodeNameException if the folder name is empty
     * @throws DotNameException if the folder name is "."
     * @throws DoubleDotNameException if the folder name is ".."
     * @throws InvalidNodeNameException if the folder name contains invalid characters
     * @throws NodeNameAlreadyExistsException if a folder with the same name already exists
     */
    suspend operator fun invoke(name: String, parentNodeId: NodeId?) {
        validateNodeNameCharactersUseCase(name)
        if (checkFolderNameExists(name, parentNodeId)) throw NodeNameAlreadyExistsException()
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
}
