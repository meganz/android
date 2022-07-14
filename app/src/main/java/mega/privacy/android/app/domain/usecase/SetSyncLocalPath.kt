package mega.privacy.android.app.domain.usecase

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
