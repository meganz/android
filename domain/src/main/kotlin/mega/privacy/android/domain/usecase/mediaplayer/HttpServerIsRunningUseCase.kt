package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import javax.inject.Inject

/**
 * The use case for checking whether the http server is running
 */
class HttpServerIsRunningUseCase @Inject constructor(
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
) {

    /**
     * Check whether the http server is running
     *
     * @param isFolderLink whether the folder link feature
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend operator fun invoke(isFolderLink: Boolean): Int =
        if (isFolderLink && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerIsRunningUseCase()
        } else {
            megaApiHttpServerIsRunningUseCase()
        }
}