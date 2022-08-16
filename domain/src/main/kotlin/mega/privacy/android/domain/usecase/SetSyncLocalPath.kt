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
    suspend operator fun invoke(localPath: String)
}
