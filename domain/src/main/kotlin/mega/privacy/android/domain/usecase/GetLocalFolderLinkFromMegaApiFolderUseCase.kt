package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case of getting local folder for folder link from mega api folder
 */
class GetLocalFolderLinkFromMegaApiFolderUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get local folder for folder link from mega api folder
     *
     * @param handle mega handle of current item
     * @return folder link
     */
    suspend operator fun invoke(handle: Long) =
        mediaPlayerRepository.getLocalLinkForFolderLinkFromMegaApiFolder(handle)
}