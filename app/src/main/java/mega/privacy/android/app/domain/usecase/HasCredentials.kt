package mega.privacy.android.app.domain.usecase

/**
 * Use case to check if credentials exist
 */
fun interface HasCredentials {

    /**
     * Invoke
     *
     * @return do credentials exist
     */
    operator fun invoke(): Boolean
}
