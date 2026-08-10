package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if a given node / file name corresponds to a text file type.
 *
 * @param fileSystemRepository The repository for file system operations.
 * @throws InvalidNodeExtensionException If the file name does not have a valid text file extension.
 */
class IsValidTextFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    operator fun invoke(fileName: String) {
        val isTextFileType =
            fileSystemRepository.getFileTypeInfoByName(fileName) is TextFileTypeInfo
        if (isTextFileType.not()) {
            throw InvalidNodeExtensionException()
        }
    }

    operator fun invoke(node: Node) = invoke(node.name)
}
