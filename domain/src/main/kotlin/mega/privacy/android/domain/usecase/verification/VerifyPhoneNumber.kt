package mega.privacy.android.domain.usecase.verification

/**
 * Verify phone number
 */
fun interface VerifyPhoneNumber {
    /**
     * Invoke
     *
     * @param pin
     */
    suspend operator fun invoke(pin: String)
}