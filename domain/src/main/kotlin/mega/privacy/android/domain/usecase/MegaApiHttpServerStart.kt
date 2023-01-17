package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApi http server starts
 */
fun interface MegaApiHttpServerStart {

    /**
     * MegaApi http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend operator fun invoke(): Boolean
}