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
     * logout
     */
    suspend fun logout()

    /**
     * logout
     */
    suspend fun sendSMSVerificationCode(phoneNumber: String)
}
