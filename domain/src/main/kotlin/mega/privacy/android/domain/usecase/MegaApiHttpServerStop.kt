package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApi http server stop
 */
fun interface MegaApiHttpServerStop {

    /**
     * MegaApi http server stop
     */
    suspend operator fun invoke()
}