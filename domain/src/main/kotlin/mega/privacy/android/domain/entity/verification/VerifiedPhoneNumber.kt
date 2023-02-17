package mega.privacy.android.domain.entity.verification


/**
 * Verified phone number
 */
sealed interface VerifiedPhoneNumber {
    /**
     * No verified phone number present
     */
    object NoVerifiedPhoneNumber : VerifiedPhoneNumber

    /**
     * Phone number
     *
     * @property phoneNumberString
     */
    @JvmInline
    value class PhoneNumber(val phoneNumberString: String) : VerifiedPhoneNumber
}
