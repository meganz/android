package mega.privacy.android.domain.usecase

/**
 * Initialise logging
 *
 */
interface InitialiseLogging {
    /**
     * Invoke
     *
     * @param overrideEnabledSettings Forces logging enabled if true regardless of user settings
     */
    suspend operator fun invoke(overrideEnabledSettings: Boolean)
}