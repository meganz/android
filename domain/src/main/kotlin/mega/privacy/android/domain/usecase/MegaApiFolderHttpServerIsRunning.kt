package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApiFolder http server whether is running
 */
fun interface MegaApiFolderHttpServerIsRunning {

    /**
     * MegaApiFolder http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend operator fun invoke(): Int
}