package mega.privacy.android.domain.usecase.account

/**
 * Set latest target path of move/copy
 */
fun interface SetLatestTargetPath {
    /**
     * Invoke
     */
    suspend operator fun invoke(path: Long)
}