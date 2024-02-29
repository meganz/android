package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get local file for node
 *
 * @property fileSystemRepository File system repository
 */
class GetLocalFileForNodeUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param fileNode File node
     */
    suspend operator fun invoke(fileNode: FileNode) =
        fileSystemRepository.getLocalFile(fileNode)
}