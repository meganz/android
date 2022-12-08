package mega.privacy.android.domain.usecase

/**
 * Send SMS verification code use case
 */
fun interface SendSMSVerificationCode {
    /**
     * invoke
     */
    suspend operator fun invoke(phoneNumber: String)
}
