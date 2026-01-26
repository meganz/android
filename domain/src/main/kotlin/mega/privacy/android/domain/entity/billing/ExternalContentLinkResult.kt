package mega.privacy.android.domain.entity.billing

/**
 * Result of launching an external content link.
 *
 * @property Success The operation succeeded and the link was approved
 * @property Cancelled The user cancelled the operation
 * @property Failed The operation failed due to an error
 */
sealed class ExternalContentLinkResult {
    /**
     * The operation succeeded and the link was approved
     */
    data object Success : ExternalContentLinkResult()

    /**
     * The user cancelled the operation
     */
    data object Cancelled : ExternalContentLinkResult()

    /**
     * The operation failed due to an error
     */
    data class Failed(val errorMessage: String? = null) : ExternalContentLinkResult()
}
