package mega.privacy.android.domain.usecase

/**
 * Set sync record pending by local path
 *
 */
interface SetSyncRecordPendingByPath {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(localPath: String?, isSecondary: Boolean)
}
