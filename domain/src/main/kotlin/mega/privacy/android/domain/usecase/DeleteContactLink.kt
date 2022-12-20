package mega.privacy.android.domain.usecase

/**
 * Use case to delete a contact link
 *

 */
fun interface DeleteContactLink {
    /**
     * Invoke method
     *
     * @param handle   Handle of the contact link to delete.
     * If the parameter is INVALID_HANDLE, the active contact link is deleted
     */
    suspend operator fun invoke(handle: Long)
}