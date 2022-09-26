package mega.privacy.android.domain.usecase


/**
 * Use Case that returns Boolean when the node is in rubbish.
 */
fun interface CheckNodeInRubbish {

    /**
     * @param handle
     * @return Boolean that determines whether node is in rubbish or not
     */
    suspend operator fun invoke(handle: Long): Boolean
}