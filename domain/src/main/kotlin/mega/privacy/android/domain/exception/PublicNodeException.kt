package mega.privacy.android.domain.exception

/**
 * Public Node Exception
 */
sealed class PublicNodeException : RuntimeException("PublicNodeException") {

    /**
     * API_ETOOMANY result from sdk
     */
    class AccountTerminated : PublicNodeException()

    /**
     * API_EBLOCKED result from sdk
     */

    class LinkRemoved : PublicNodeException()

    /**
     * API_EINCOMPLETE result from sdk
     */
    class DecryptionKeyRequired : PublicNodeException()

    /**
     * Invalid key
     */

    class InvalidDecryptionKey : PublicNodeException()

    /**
     * Generic error
     */
    class GenericError : PublicNodeException()
}