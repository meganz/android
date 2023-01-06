package mega.privacy.android.domain.repository

/**
 * Verification repository
 */
interface VerificationRepository {

    /**
     * Set the status for SMSVerification
     */
    suspend fun setSMSVerificationShown(isShown: Boolean)

    /**
     * check whether SMS Verification Shown or not
     */
    suspend fun isSMSVerificationShown(): Boolean

    /**
     * get country calling codes
     */
    suspend fun getCountryCallingCodes(): List<String>

    /**
     * send SMS Verification Code
     */
    suspend fun sendSMSVerificationCode(phoneNumber: String)

    /**
     * reset sms verified phone number
     */
    suspend fun resetSMSVerifiedPhoneNumber()

    /**
     * get current country code based on the network
     */
    suspend fun getCurrentCountryCode(): String?

    /**
     * check whether device is in roaming state
     */
    suspend fun isRoaming(): Boolean

    /**
     * format phone number
     * @param number
     * @param countryCode
     */
    suspend fun formatPhoneNumber(number: String, countryCode: String): String?
}
