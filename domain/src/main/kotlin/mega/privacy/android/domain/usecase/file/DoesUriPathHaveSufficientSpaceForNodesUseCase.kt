package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if the destination have sufficient space for all these nodes
 * Notice that it will work for paths, file Uri or external content Uri, but not for other types of Uris
 */
class DoesUriPathHaveSufficientSpaceForNodesUseCase @Inject constructor(
    private val totalFileSizeOfNodesUseCase: TotalFileSizeOfNodesUseCase,
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param destinationUriPath where the space will be checked, it should be a path, file Uri or external content Uri
     * @param nodes the list of nodes to get the required space
     * @return true if path has sufficient space, otherwise false
     */
    suspend operator fun invoke(destinationUriPath: UriPath, nodes: List<TypedNode>): Boolean =
        doesPathHaveSufficientSpaceUseCase(
            path = when {
                fileSystemRepository.isExternalStorageContentUri(destinationUriPath.value) ->
                    fileSystemRepository.getExternalPathByContentUri(destinationUriPath.value)

                fileSystemRepository.isFileUri(destinationUriPath.value) ->
                    fileSystemRepository.getFileFromFileUri(destinationUriPath.value).absolutePath

                else -> null
            } ?: destinationUriPath.value,
            requiredSpace = totalFileSizeOfNodesUseCase(nodes)
        )


}