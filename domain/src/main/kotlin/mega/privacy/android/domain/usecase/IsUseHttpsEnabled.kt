package mega.privacy.android.domain.usecase

/**
 * Is use https preference enabled
 *
 */
fun interface IsUseHttpsEnabled {
    /**
     * Invoke the use case
     *
     * @return the current value of the preference
     */
    suspend operator fun invoke(): Boolean
}
