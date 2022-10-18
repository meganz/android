package mega.privacy.android.app.domain.usecase

/**
 * Check if a MegaNode is pending to be shared with another User.
 * This situation happens when a node is to be shared with a User which is not a contact yet.
 */
fun interface IsPendingShare {
    /**
     * Check if a MegaNode is pending to be shared with another User.
     *
     * @param handle
     * @return true is the MegaNode is pending to be shared, otherwise false
     */
    suspend operator fun invoke(handle: Long): Boolean

}