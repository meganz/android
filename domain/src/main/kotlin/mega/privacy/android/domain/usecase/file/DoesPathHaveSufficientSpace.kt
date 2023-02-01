package mega.privacy.android.domain.usecase.file

/**
 * Does path have sufficient space
 */
fun interface DoesPathHaveSufficientSpace {
    /**
     * Invoke
     *
     * @param path
     * @param requiredSpace
     * @return true if path has sufficient space, otherwise false
     */
    suspend operator fun invoke(path: String, requiredSpace: Long): Boolean
}