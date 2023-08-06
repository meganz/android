package mega.privacy.android.domain.exception

/**
 * Exception for reset password link
 */
sealed class ResetPasswordLinkException(message: String?) : Throwable(message) {
    /**
     * The link is no longer available or expired
     */
    object LinkExpired : ResetPasswordLinkException("ResetPasswordLinkException: Link is expired")

    /**
     * The link is invalid or not related to reset password link
     */
    object LinkInvalid :
        ResetPasswordLinkException("ResetPasswordLinkException: Link is invalid or not related to reset password link")

    /**
     * Other unknown error
     * @property megaException Exception required for getting the translated error string.
     */
    class Unknown(val megaException: MegaException) :
        ResetPasswordLinkException(megaException.message ?: "QueryResetPasswordLinkException")
}