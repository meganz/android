package mega.privacy.android.domain.usecase


/**
 * Check if node is in rubbish or deleted
 */
fun interface IsNodeInRubbishOrDeleted {

    /**
     * @param nodeHandle
     * @return if node is in rubbish or deleted (null)
     */
    suspend operator fun invoke(nodeHandle: Long): Boolean
}
