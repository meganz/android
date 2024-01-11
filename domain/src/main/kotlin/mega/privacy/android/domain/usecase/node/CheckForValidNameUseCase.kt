package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.RegexRepository
import javax.inject.Inject

/**
 * While renaming node, this use case will check for all errors
 * for input text
 */
class CheckForValidNameUseCase @Inject constructor(
    private val nodeExistsInParentUseCase: NodeExistsInParentUseCase,
    private val regexRepository: RegexRepository,
) {

    /**
     * Invoke
     * @param newName New Name for node
     * @param node Node on which rename operation is performed
     * @return [ValidNameType]
     */
    suspend operator fun invoke(newName: String, node: Node): ValidNameType {
        return when {
            newName.isBlank() -> ValidNameType.BLANK_NAME
            regexRepository.invalidNamePattern.matcher(newName).find() -> ValidNameType.INVALID_NAME
            nodeExistsInParentUseCase(node, newName) -> ValidNameType.NAME_ALREADY_EXISTS
            else -> checkForExtension(newName, node)
        }
    }

    private fun checkForExtension(newName: String, node: Node): ValidNameType {
        return when (node) {
            is FileNode -> {
                val extension = getFileExtension(newName)
                when {
                    extension.isBlank() -> ValidNameType.NO_EXTENSION
                    node.type.extension.equals(extension, true)
                        .not() -> ValidNameType.DIFFERENT_EXTENSION

                    else -> ValidNameType.NO_ERROR
                }
            }

            else -> {
                ValidNameType.NO_ERROR
            }
        }
    }

    private fun getFileExtension(newName: String) =
        newName.substringAfterLast('.', "")
}

/**
 * Different values for Valid Name when user tries to update name for node
 */
enum class ValidNameType {

    /**
     * When no name
     */
    BLANK_NAME,

    /**
     * When name contains some invalid characters
     */
    INVALID_NAME,

    /**
     * Same name already exists
     */
    NAME_ALREADY_EXISTS,

    /**
     * Name changed for file by removing extension
     */
    NO_EXTENSION,

    /**
     * Original Extension for file has been changed
     */
    DIFFERENT_EXTENSION,

    /**
     * Everything is good to change name
     */
    NO_ERROR
}