package mega.privacy.android.domain.usecase.mediaplayer

import javax.inject.Inject

/**
 * The use case for stopping the http server
 */
class HttpServerStopUseCase @Inject constructor(
    private val megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase,
    private val megaApiHttpServerStopUseCase: MegaApiHttpServerStopUseCase,
) {

    /**
     * Stop the http server
     */
    suspend operator fun invoke() {
        megaApiFolderHttpServerStopUseCase()
        megaApiHttpServerStopUseCase()
    }
}