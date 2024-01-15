package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting local file path
 */
class GetLocalFilePathUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get the local folder path
     *
     * @param typedFileNode [TypedFileNode]
     * @return local file if it exists
     */
    suspend operator fun invoke(typedNode: TypedNode?) =
        if (typedNode is TypedFileNode)
            mediaPlayerRepository.getLocalFilePath(typedNode)
        else null
}