package mega.privacy.android.domain.entity.verification

/**
 * Verification status
 *
 * @property phoneNumber
 * @property canRequestUnblockSms
 * @property canRequestOptInVerification
 */
sealed interface VerificationStatus {
    val canRequestOptInVerification: Boolean
    val canRequestUnblockSms: Boolean
    val phoneNumber: VerifiedPhoneNumber
}

/**
 * Verified
 *
 * @property phoneNumber
 * @property canRequestUnblockSms
 * @property canRequestOptInVerification
 */
data class Verified(
    override val phoneNumber: VerifiedPhoneNumber.PhoneNumber,
    override val canRequestUnblockSms: Boolean,
    override val canRequestOptInVerification: Boolean,
) : VerificationStatus

/**
 * Un verified
 *
 * @property canRequestUnblockSms
 * @property canRequestOptInVerification
 */
data class UnVerified(
    override val canRequestUnblockSms: Boolean,
    override val canRequestOptInVerification: Boolean,
) : VerificationStatus {
    override val phoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
}



