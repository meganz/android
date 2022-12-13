package mega.privacy.android.domain.usecase

/**
 * Get Country Calling Codes
 */
fun interface GetCountryCallingCodes {

    /**
     * invoke
     * @return list of country codes
     */
    suspend operator fun invoke(): List<String>
}
