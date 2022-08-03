package mega.privacy.android.domain.usecase

/**
 * Set sync folder path
 *
 */
fun interface SetSyncLocalPath {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(localPath: String)
}
