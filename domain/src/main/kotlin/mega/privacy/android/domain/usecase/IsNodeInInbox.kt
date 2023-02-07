package mega.privacy.android.domain.usecase


/**
 * Use Case that returns true when the node is in the inbox.
 */
fun interface IsNodeInInbox {

    /**
     * @param handle
     * @return Boolean that determines whether the node is in the inbox or not
     */
    suspend operator fun invoke(handle: Long): Boolean
}