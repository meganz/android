package mega.privacy.android.domain.exception

/**
 * Exception for reset password link
 */
sealed class ResetPasswordLinkException(message: String?) : Throwable(message) {
    /**
     * The link is no longer available or expired
     */
    data object LinkExpired :
        ResetPasswordLinkException("ResetPasswordLinkException: Link is expired")

    /**
     * The link is invalid or not related to reset password link
     */
    data object LinkInvalid :
        ResetPasswordLinkException("ResetPasswordLinkException: Link is invalid or not related to reset password link")

    /**
     * The link access denied
     */
    data object LinkAccessDenied :
        ResetPasswordLinkException("ResetPasswordLinkException: Link access denied")
}