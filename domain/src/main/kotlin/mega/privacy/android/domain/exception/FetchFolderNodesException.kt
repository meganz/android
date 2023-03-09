package mega.privacy.android.domain.exception

/**
 * Fetch Folder Nodes exception
 */
sealed class FetchFolderNodesException : RuntimeException("FetchFolderNodesException") {
    /**
     * API_ETOOMANY result from sdk
     */
    class AccountTerminated : FetchFolderNodesException()

    /**
     * API_EBLOCKED result from sdk
     */

    class LinkRemoved : FetchFolderNodesException()

    /**
     * Invalid key
     */

    class InvalidDecryptionKey : FetchFolderNodesException()

    /**
     * Generic error
     */
    class GenericError : FetchFolderNodesException()
}
