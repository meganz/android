package mega.privacy.android.domain.usecase

/**
 * Set sync record pending by local path
 *
 */
fun interface SetSyncRecordPendingByPath {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(localPath: String?, isSecondary: Boolean)
}
