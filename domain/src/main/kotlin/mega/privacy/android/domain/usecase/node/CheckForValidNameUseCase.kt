package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.RegexRepository
import javax.inject.Inject

/**
 * While renaming or creating a node, this use case will check for all errors for input text
 */
class CheckForValidNameUseCase @Inject constructor(
    private val nodeExistsInParentUseCase: NodeExistsInParentUseCase,
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    private val regexRepository: RegexRepository,
) {

    /**
     * Invoke
     * @param newName New Name for node
     * @param node Node on which rename operation is performed
     * @return [InvalidNameType]
     */
    suspend operator fun invoke(newName: String, node: Node): InvalidNameType {
        return when {
            newName.isBlank() -> InvalidNameType.BLANK_NAME
            newName.isInvalidDotName() -> InvalidNameType.DOT_NAME
            newName.isInvalidDoubleDotName() -> InvalidNameType.DOUBLE_DOT_NAME
            regexRepository.invalidNamePattern.matcher(newName)
                .find() -> InvalidNameType.INVALID_NAME

            node is FolderNode && nodeExistsInCurrentLocationUseCase(node.id, newName)
                -> InvalidNameType.NAME_ALREADY_EXISTS

            nodeExistsInParentUseCase(node, newName) -> InvalidNameType.NAME_ALREADY_EXISTS

            node is FileNode -> {
                val extension = newName.substringAfterLast('.', "")

                when {
                    extension.isBlank() -> InvalidNameType.NO_EXTENSION
                    extension.equals(node.type.extension, true)
                        .not() -> InvalidNameType.DIFFERENT_EXTENSION

                    else -> InvalidNameType.VALID
                }
            }

            else -> InvalidNameType.VALID
        }
    }

    companion object {
        fun String.isInvalidDotName() = this == "."

        fun String.isInvalidDoubleDotName() = this == ".."
    }
}
