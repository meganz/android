package mega.privacy.android.domain.usecase

/**
 * Set sync folder path
 *
 */
interface SetSyncLocalPath {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(localPath: String)
}
