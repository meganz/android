package mega.privacy.android.app.domain.usecase

/**
 * Initialise logging
 *
 */
fun interface InitialiseLogging {
    /**
     * Invoke
     *
     * @param isDebug
     */
    suspend operator fun invoke(isDebug: Boolean)
}