package mega.privacy.android.domain.exception

/**
 * Query signup link exception.
 */
sealed class QuerySignupLinkException : RuntimeException("QuerySignupLinkException") {

    /**
     * The link is no longer available.
     */
    object LinkNoLongerAvailable : QuerySignupLinkException()

    /**
     * Other error.
     *
     * @property megaException Exception required for getting the translated error string.
     */
    class Unknown(val megaException: MegaException) : QuerySignupLinkException()
}