package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject

/**
 * The use case of getting local folder for folder link from mega api
 */
class GetLocalFolderLinkFromMegaApiUseCase @Inject constructor(
    private val streamingServerRepository: StreamingServerRepository,
) {

    /**
     * Get local folder for folder link from mega api
     *
     * @param handle mega handle of current item
     * @return folder link
     */
    suspend operator fun invoke(handle: Long) =
        streamingServerRepository.getFolderLinkFileStreamingUri(handle)
}