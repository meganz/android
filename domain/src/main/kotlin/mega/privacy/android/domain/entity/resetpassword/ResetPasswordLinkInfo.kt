package mega.privacy.android.domain.entity.resetpassword

/**
 * Reset password link information
 *
 * @property email Email associated with the reset password link
 * @property isRequiredRecoveryKey Flag indicating if recovery key is required for the reset password link
 */
data class ResetPasswordLinkInfo(
    val email: String,
    val isRequiredRecoveryKey: Boolean,
)
