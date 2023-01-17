package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApiFolder http server starts
 */
fun interface MegaApiFolderHttpServerStart {

    /**
     * MegaApiFolder http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend operator fun invoke(): Boolean
}