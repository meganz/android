package mega.privacy.android.domain.usecase.verification

/**
 * Format phone number
 */
fun interface FormatPhoneNumber {
    /**
     * Invoke
     *
     * @param phoneNumber
     * @param countryCode
     */
    suspend operator fun invoke(phoneNumber: String, countryCode: String): String?
}
