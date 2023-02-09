package mega.privacy.android.domain.entity


/**
 * Verified phone number
 */
sealed interface VerifiedPhoneNumber {
    /**
     * No verified phone number present
     */
    object NoVerifiedPhoneNumber : VerifiedPhoneNumber

    @JvmInline
    value class PhoneNumber(val phoneNumberString: String) : VerifiedPhoneNumber
}
