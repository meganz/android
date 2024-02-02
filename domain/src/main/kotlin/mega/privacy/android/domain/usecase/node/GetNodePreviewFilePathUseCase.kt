package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get local file path of a node
 *
 * if local file does not exists checks if there is a preview file in cache
 */
class GetNodePreviewFilePathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val cacheRepository: CacheRepository,
) {

    /**
     * Invoke
     * @param node [Node]
     */
    suspend operator fun invoke(node: TypedFileNode) =
        fileSystemRepository.getLocalFile(node)?.path
            .takeIf { it != null && fileSystemRepository.doesFileExist(it) }
            ?: cacheRepository.getFilePreviewPath(node.name)
                .takeIf { fileSystemRepository.doesFileExist(it) }
}