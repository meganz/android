package mega.privacy.android.domain.usecase

/**
 * Delete camera upload sync record by local path
 *
 */
fun interface DeleteSyncRecordByLocalPath {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(localPath: String, isSecondary: Boolean)
}
