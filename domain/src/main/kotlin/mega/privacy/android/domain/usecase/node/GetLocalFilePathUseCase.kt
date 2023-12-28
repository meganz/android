package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get local file path of a node
 */
class GetLocalFilePathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     * @param node [Node]
     */
    suspend operator fun invoke(node: Node): String? {
        return if (node is FileNode) {
            fileSystemRepository.getLocalFile(node)?.path
        } else {
            null
        }
    }
}