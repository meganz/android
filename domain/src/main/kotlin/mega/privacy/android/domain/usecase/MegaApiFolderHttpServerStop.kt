package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApiFolder http server stop
 */
fun interface MegaApiFolderHttpServerStop {

    /**
     * MegaApiFolder http server stop
     */
    suspend operator fun invoke()
}