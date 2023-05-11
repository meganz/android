package mega.privacy.android.domain.usecase


/**
 * Use Case for getting Either Primary or Secondary Folder Handle
 */
@Deprecated("Use GetPrimaryFolderSyncHandleUseCase or GetSecondarySyncHandleUseCase instead")
fun interface GetUploadFolderHandle {

    /**
     * @param isPrimary whether the primary upload's folder is returned
     * @return the primary or secondary upload folder's handle
     */
    suspend operator fun invoke(isPrimary: Boolean): Long
}
