package mega.privacy.android.domain.repository

/**
 * SMS Verification repository
 */
interface SMSVerificationRepository {

    /**
     * Set the status for SMSVerification
     */
    suspend fun setSMSVerificationShown(isShown: Boolean)

    /**
     * check whether SMS Verification Shown or not
     */
    suspend fun isSMSVerificationShown(): Boolean
}
