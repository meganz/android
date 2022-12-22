package mega.privacy.android.domain.usecase

/**
 * Set My Chat Files Folder
 */
fun interface SetMyChatFilesFolder {
    /**
     * invoke
     * @param handle
     * @return [Long]
     */
    suspend operator fun invoke(handle: Long): Long?
}
