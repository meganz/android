package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import javax.inject.Inject

/**
 * The use case for starting the http server
 */
class HttpServerStartUseCase @Inject constructor(
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
) {

    /**
     * Start the http server
     *
     * @param isFolderLink whether the folder link feature
     * @return True if the server is ready, false if the initialization failed
     */
    suspend operator fun invoke(isFolderLink: Boolean): Boolean =
        if (isFolderLink && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerStartUseCase()
        } else {
            megaApiHttpServerStartUseCase()
        }
}