package mega.privacy.android.domain.usecase

/**
 * Reset sms verified phone number
 */
fun interface ResetSMSVerifiedPhoneNumber {

    /**
     * invoke
     */
    suspend operator fun invoke()
}
