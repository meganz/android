package mega.privacy.android.app.smsVerification.model.mapper

/**
 * Sms verification text error mapper
 */
fun interface SmsVerificationTextErrorMapper {
    /**
     * Invoke
     *
     * @param error
     * @return the error string to display
     */
    operator fun invoke(error: Throwable): String
}


