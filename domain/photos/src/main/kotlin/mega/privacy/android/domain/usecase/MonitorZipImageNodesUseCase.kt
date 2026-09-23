package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to monitor zip nodes
 */
class MonitorZipImageNodesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val photosRepository: PhotosRepository,
) {
    operator fun invoke(uriString: String): Flow<List<ImageNode>> = flow {
        emit(populateNodes(uriString))
    }

    private suspend fun populateNodes(uriString: String): List<ImageNode> {
        val files = fileSystemRepository.getFileSiblingByUri(uriString)
        return photosRepository.getImageNodesInFiles(files)
    }
}
