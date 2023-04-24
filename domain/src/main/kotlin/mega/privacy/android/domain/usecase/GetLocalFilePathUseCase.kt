package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedFileNode
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
    suspend operator fun invoke(typedFileNode: TypedFileNode?) =
        mediaPlayerRepository.getLocalFilePath(typedFileNode)
}