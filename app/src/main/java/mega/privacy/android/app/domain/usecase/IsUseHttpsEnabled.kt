package mega.privacy.android.app.domain.usecase

/**
 * Is use https preference enabled
 *
 */
interface IsUseHttpsEnabled {
    /**
     * Invoke the use case
     *
     * @return the current value of the preference
     */
    suspend operator fun invoke(): Boolean
}
