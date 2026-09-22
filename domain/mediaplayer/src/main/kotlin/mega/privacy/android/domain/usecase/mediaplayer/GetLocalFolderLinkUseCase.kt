package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import javax.inject.Inject

/**
 * The use case of getting local folder link
 */
class GetLocalFolderLinkUseCase @Inject constructor(
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
) {

    /**
     * Get local folder link
     *
     * @param handle mega handle of current item
     * @return folder link
     */
    suspend operator fun invoke(handle: Long) =
        if (hasCredentialsUseCase()) {
            getLocalFolderLinkFromMegaApiUseCase(handle)
        } else {
            getLocalFolderLinkFromMegaApiFolderUseCase(handle)
        }
}