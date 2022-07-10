package mega.privacy.android.domain.usecase

/**
 * Set the value of the use https preference
 *
 */
fun interface SetUseHttps {
    /**
     * Invoke the use case
     *
     * @param enabled new value for the preference
     * @return the latest value of the preference
     */
    suspend operator fun invoke(enabled: Boolean): Boolean
}
