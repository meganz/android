package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.verification.SmsPermission
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber

/**
 * Verification repository
 *
 * @constructor Create empty Verification repository
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

    /**
     * Monitor verified phone number
     *
     * @return flow of the verified phone number response
     */
    fun monitorVerifiedPhoneNumber(): Flow<VerifiedPhoneNumber>

    /**
     * Verify phone number
     *
     * @param pin
     */
    suspend fun verifyPhoneNumber(pin: String)

    /**
     * Get sms permissions
     *
     * @return list of allowed sms requests
     */
    suspend fun getSmsPermissions(): List<SmsPermission>
}
